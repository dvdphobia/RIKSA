package com.riksa.social.service;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.social.dto.CategoryResponse;
import com.riksa.social.dto.CommentPage;
import com.riksa.social.dto.CommentResponse;
import com.riksa.social.dto.PageInfo;
import com.riksa.social.dto.PostResponse;
import com.riksa.social.dto.SocialMapper;
import com.riksa.social.dto.UserResponse;
import com.riksa.social.entity.Category;
import com.riksa.social.entity.Comment;
import com.riksa.social.entity.Post;
import com.riksa.social.entity.Reaction;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.enums.ReactionType;
import com.riksa.social.repository.CommentRepository;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the cross-domain fields on PostResponse / CommentResponse that the
 * GraphQL schema declares but the existing controllers were leaving null:
 *   - Post.author      (User from auth domain)
 *   - Post.myReaction   (current user's reaction on the post)
 *   - Post.comments     (nested CommentPage)
 *   - Comment.author    (User from auth domain)
 *   - Comment.myReaction (current user's reaction on the comment)
 */
@Service
@RequiredArgsConstructor
public class SocialResponseResolver {

    private final UserRepository userRepository;
    private final ReactionRepository reactionRepository;
    private final CommentRepository commentRepository;
    private final CategoryService categoryService;
    private final PostRepository postRepository;

    // ── Post ──────────────────────────────────────────────

    public PostResponse resolvePost(Post post, UUID currentUserId) {
        return SocialMapper.toPostResponse(
                post,
                resolveUser(post.getAuthorId()),
                resolveCategory(post.getCategoryId(), currentUserId),
                resolveMyReaction(currentUserId, post.getId(), null)
        );
    }

    public PostResponse resolvePostWithComments(Post post, UUID currentUserId, int commentLimit) {
        return SocialMapper.toPostResponse(
                post,
                resolveUser(post.getAuthorId()),
                resolveCategory(post.getCategoryId(), currentUserId),
                resolveMyReaction(currentUserId, post.getId(), null)
        );
    }

    public List<PostResponse> resolvePosts(List<Post> posts, UUID currentUserId) {
        if (posts.isEmpty()) return List.of();

        Set<UUID> authorIds = posts.stream()
                .map(Post::getAuthorId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, UserResponse> usersById = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, SocialMapper::toUserResponse));

        Set<UUID> categoryIds = posts.stream()
                .map(Post::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, Category> categoriesById = categoryService.listCategories().stream()
                .filter(category -> categoryIds.contains(category.getId()))
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<UUID, CategoryResponse> categoryResponsesById = categoriesById.values().stream()
                .collect(Collectors.toMap(Category::getId, category -> SocialMapper.toCategoryResponse(
                        category,
                        categoryService.getMemberCount(category.getId()),
                        categoryService.isMember(currentUserId, category.getId()))));

        Map<UUID, ReactionType> reactionsByPostId = Map.of();
        if (currentUserId != null) {
            List<UUID> postIds = posts.stream().map(Post::getId).toList();
            reactionsByPostId = reactionRepository
                    .findByUserIdAndPostIdInAndCommentIdIsNull(currentUserId, postIds).stream()
                    .collect(Collectors.toMap(Reaction::getPostId, Reaction::getType));
        }

        Map<UUID, ReactionType> finalReactionsByPostId = reactionsByPostId;
        return posts.stream()
                .map(post -> SocialMapper.toPostResponse(
                        post,
                        usersById.get(post.getAuthorId()),
                        categoryResponsesById.get(post.getCategoryId()),
                        finalReactionsByPostId.get(post.getId())))
                .toList();
    }

    // ── Comments ─────────────────────────────────────────

    public CommentPage resolveComments(UUID postId, UUID currentUserId, int limit) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
        if (post.getStatus() == ContentStatus.HIDDEN) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }

        List<Comment> comments = commentRepository
                .findByPostIdAndParentIdIsNullAndStatusOrderByCreatedAtAsc(postId, ContentStatus.VISIBLE, PageRequest.of(0, limit + 1));

        boolean hasNext = comments.size() > limit;
        if (hasNext) comments = comments.subList(0, limit);

        List<CommentResponse> edges = comments.stream()
                .map(c -> resolveComment(c, currentUserId))
                .toList();

        UUID endCursor = hasNext && !comments.isEmpty() ? comments.get(comments.size() - 1).getId() : null;
        return new CommentPage(edges, new PageInfo(hasNext, endCursor));
    }

    public CommentPage resolveReplies(UUID parentId, UUID currentUserId, int limit) {
        List<Comment> replies = commentRepository
                .findByParentIdAndStatusOrderByCreatedAtAsc(parentId, ContentStatus.VISIBLE, PageRequest.of(0, limit + 1));

        boolean hasNext = replies.size() > limit;
        if (hasNext) replies = replies.subList(0, limit);

        List<CommentResponse> edges = replies.stream()
                .map(c -> resolveComment(c, currentUserId))
                .toList();

        UUID endCursor = hasNext && !replies.isEmpty() ? replies.get(replies.size() - 1).getId() : null;
        return new CommentPage(edges, new PageInfo(hasNext, endCursor));
    }

    public CommentResponse resolveComment(Comment comment, UUID currentUserId) {
        return SocialMapper.toCommentResponse(
                comment,
                resolveUser(comment.getAuthorId()),
                resolveMyReaction(currentUserId, null, comment.getId())
        );
    }

    // ── Helpers ──────────────────────────────────────────

    public UserResponse resolveUser(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(SocialMapper::toUserResponse)
                .orElse(null);
    }

    public CategoryResponse resolveCategory(UUID categoryId, UUID currentUserId) {
        if (categoryId == null) return null;
        return categoryService.listCategories().stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst()
                .map(c -> SocialMapper.toCategoryResponse(
                        c,
                        categoryService.getMemberCount(c.getId()),
                        categoryService.isMember(currentUserId, c.getId())))
                .orElse(null);
    }

    public ReactionType resolveMyReaction(UUID currentUserId, UUID postId, UUID commentId) {
        if (currentUserId == null) return null;
        Optional<Reaction> reaction = reactionRepository
                .findByUserIdAndPostIdAndCommentId(currentUserId, postId, commentId);
        return reaction.map(Reaction::getType).orElse(null);
    }
}
