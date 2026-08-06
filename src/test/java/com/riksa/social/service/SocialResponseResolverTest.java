package com.riksa.social.service;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.social.entity.Category;
import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.entity.Reaction;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.enums.ReactionType;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.ReactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialResponseResolverTest {
    @Mock UserRepository userRepository;
    @Mock ReactionRepository reactionRepository;
    @Mock CommentRepository commentRepository;
    @Mock CategoryService categoryService;
    @Mock PostRepository postRepository;

    private SocialResponseResolver resolver() {
        return new SocialResponseResolver(userRepository, reactionRepository, commentRepository, categoryService, postRepository);
    }

    @Test
    void resolvePostsBatchLoadsUsersCategoriesAndReactions() {
        UUID userId = UUID.randomUUID(), authorId = UUID.randomUUID(), categoryId = UUID.randomUUID();
        Post first = post(authorId, categoryId), second = post(authorId, categoryId);
        User author = new User(); author.setId(authorId); author.setUsername("author"); author.setDisplayName("Author");
        Category category = new Category(); category.setId(categoryId); category.setName("Science"); category.setSlug("science");
        Reaction reaction = new Reaction(); reaction.setPostId(first.getId()); reaction.setType(ReactionType.UPVOTE);
        when(userRepository.findAllById(any())).thenReturn(List.of(author));
        when(categoryService.listCategories()).thenReturn(List.of(category));
        when(categoryService.getMemberCount(categoryId)).thenReturn(4);
        when(categoryService.isMember(userId, categoryId)).thenReturn(true);
        when(reactionRepository.findByUserIdAndPostIdInAndCommentIdIsNull(eq(userId), any()))
                .thenReturn(List.of(reaction));

        var responses = resolver().resolvePosts(List.of(first, second), userId);

        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().author().displayName()).isEqualTo("Author");
        assertThat(responses.getFirst().myReaction()).isEqualTo(ReactionType.UPVOTE);
        verify(userRepository, times(1)).findAllById(any());
        verify(categoryService, times(1)).listCategories();
        verify(reactionRepository, times(1)).findByUserIdAndPostIdInAndCommentIdIsNull(eq(userId), any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void resolvePostMapsSingleItem() {
        UUID currentUser = UUID.randomUUID(), authorId = UUID.randomUUID(), categoryId = UUID.randomUUID();
        Post post = post(authorId, categoryId);
        User author = new User(); author.setId(authorId); author.setUsername("a"); author.setDisplayName("A");
        Category category = new Category(); category.setId(categoryId); category.setName("Math"); category.setSlug("math");
        Reaction reaction = new Reaction(); reaction.setType(ReactionType.DOWNVOTE);
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(categoryService.listCategories()).thenReturn(List.of(category));
        when(reactionRepository.findByUserIdAndPostIdAndCommentId(currentUser, post.getId(), null))
                .thenReturn(Optional.of(reaction));
        assertThat(resolver().resolvePost(post, currentUser).myReaction()).isEqualTo(ReactionType.DOWNVOTE);
    }

    @Test
    void resolveCommentsAndRepliesMapPagination() {
        UUID postId = UUID.randomUUID(), parentId = UUID.randomUUID(), authorId = UUID.randomUUID();
        Post post = post(authorId, UUID.randomUUID()); post.setId(postId);
        Comment first = comment(postId, null, authorId), second = comment(postId, null, authorId);
        User author = new User(); author.setId(authorId); author.setUsername("u"); author.setDisplayName("User");
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findByPostIdAndParentIdIsNullAndStatusOrderByCreatedAtAsc(
                eq(postId), eq(ContentStatus.VISIBLE), any(Pageable.class)))
                .thenReturn(List.of(first, second));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        var page = resolver().resolveComments(postId, null, 1);
        assertThat(page.edges()).hasSize(1);
        assertThat(page.pageInfo().hasNextPage()).isTrue();
        assertThat(page.pageInfo().endCursor()).isEqualTo(first.getId());

        Comment reply = comment(postId, parentId, authorId);
        when(commentRepository.findByParentIdAndStatusOrderByCreatedAtAsc(
                eq(parentId), eq(ContentStatus.VISIBLE), any(Pageable.class)))
                .thenReturn(List.of(reply));
        assertThat(resolver().resolveReplies(parentId, null, 10).edges()).hasSize(1);
    }

    @Test
    void resolveComments_hiddenPost_throwsException() {
        UUID postId = UUID.randomUUID();
        Post post = post(UUID.randomUUID(), UUID.randomUUID());
        post.setId(postId);
        post.setStatus(ContentStatus.HIDDEN);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> resolver().resolveComments(postId, null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");

        verifyNoInteractions(commentRepository);
    }

    @Test
    void resolveComments_missingPost_throwsException() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver().resolveComments(postId, null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");
    }

    private Post post(UUID authorId, UUID categoryId) {
        Post post = new Post(); post.setId(UUID.randomUUID()); post.setAuthorId(authorId);
        post.setCategoryId(categoryId); post.setTitle("Title"); post.setContent("Content"); return post;
    }

    private Comment comment(UUID postId, UUID parentId, UUID authorId) {
        Comment comment = new Comment(); comment.setId(UUID.randomUUID()); comment.setPostId(postId);
        comment.setParentId(parentId); comment.setAuthorId(authorId); comment.setContent("Comment"); return comment;
    }
}
