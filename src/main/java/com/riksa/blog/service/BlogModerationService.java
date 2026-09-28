package com.riksa.blog.service;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.event.BlogApprovedEvent;
import com.riksa.blog.event.BlogRejectedEvent;
import com.riksa.blog.event.BlogPostApprovedEvent;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.common.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlogModerationService {
    private final BlogPostRepository blogPostRepository;
    private final EventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<BlogPost> list(BlogStatus status, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        return blogPostRepository.findByStatus(status,
                PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public BlogPost getPost(UUID postId) {
        return blogPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Blog post not found: " + postId));
    }

    @Transactional
    public BlogPost approve(UUID postId, UUID moderatorId){
        BlogPost post = getPostForUpdate(postId);
        if (post.getStatus() != BlogStatus.PENDING && post.getStatus() != BlogStatus.APPROVED) {
            throw new IllegalStateException("Post already moderated: " + post.getStatus());
        }
        return publishApproved(post, moderatorId);
    }

    /** Publishes a post and emits approval events only on its first approval. */
    @Transactional
    public BlogPost publishApproved(BlogPost post, UUID moderatorId) {
        boolean firstApproval = post.getFirstApprovedAt() == null;
        post.setStatus(BlogStatus.APPROVED);
        post.setRejectionReason(null);
        if (firstApproval) {
            post.setFirstApprovedAt(OffsetDateTime.now());
        }
        post = blogPostRepository.save(post);

        if (firstApproval) {
            eventPublisher.publish(new BlogApprovedEvent(this, post.getId(), post.getAuthorId(), moderatorId));
            eventPublisher.publish(new BlogPostApprovedEvent(this, post.getId(), post.getAuthorId()));
        }
        return post;
    }

    @Transactional
    public BlogPost reject(UUID postId, String reason, UUID moderatorId){
        BlogPost post = getPostForUpdate(postId);
        if (post.getStatus() != BlogStatus.PENDING) {
            throw new IllegalStateException("Post already moderated: " + post.getStatus());
        }
        post.setStatus(BlogStatus.REJECTED);
        post.setRejectionReason(reason);
        post = blogPostRepository.save(post);

        eventPublisher.publish(new BlogRejectedEvent(this, postId, post.getAuthorId(), moderatorId, reason));
        return post;
    }

    private BlogPost getPostForUpdate(UUID postId){
        return blogPostRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new IllegalArgumentException("Blog post not found: " + postId));
    }
}
