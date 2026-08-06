package com.riksa.social.service;

import com.riksa.common.event.EventPublisher;
import com.riksa.social.entity.Post;
import com.riksa.social.enums.ContentStatus;
import com.riksa.social.event.PostCreatedEvent;
import com.riksa.social.repository.PostRepository;
import com.riksa.social.repository.UserCategoryMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserCategoryMembershipRepository membershipRepository;
    private final ContentFilterService contentFilterService;
    private final EventPublisher eventPublisher;

    @Transactional
    public Post createPost(UUID authorId, String title, String content, String mediaUrl, UUID categoryId) {
        // must be a member of the category
        if (!membershipRepository.existsByUserIdAndCategoryId(authorId, categoryId)) {
            throw new IllegalStateException("You must join this category before posting");
        }

        // STEM content check
        contentFilterService.validate(title + " " + content);

        Post post = new Post();
        post.setAuthorId(authorId);
        post.setCategoryId(categoryId);
        post.setTitle(title);
        post.setContent(content);
        post.setMediaUrl(mediaUrl);

        Post saved = postRepository.save(post);

        eventPublisher.publish(new PostCreatedEvent(this, saved.getId(), authorId, categoryId));

        return saved;
    }

    @Transactional
    public Post updatePost(UUID authorId, UUID postId, String title, String content) {
        Post post = getPost(postId);

        if (!post.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("You can only edit your own posts");
        }

        if (title != null) post.setTitle(title);
        if (content != null) {
            contentFilterService.validate(content);
            post.setContent(content);
        }

        return postRepository.save(post);
    }

    @Transactional
    public void deletePost(UUID authorId, UUID postId) {
        Post post = getPost(postId);

        if (!post.getAuthorId().equals(authorId)) {
            throw new IllegalStateException("You can only delete your own posts");
        }

        postRepository.delete(post);
    }

    public Post getPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
    }

    public Post getVisiblePost(UUID postId) {
        Post post = getPost(postId);
        if (post.getStatus() == ContentStatus.HIDDEN) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }
        return post;
    }
}
