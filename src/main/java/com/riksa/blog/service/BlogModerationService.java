package com.riksa.blog.service;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.event.BlogApprovedEvent;
import com.riksa.blog.event.BlogRejectedEvent;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.common.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlogModerationService {
    private final BlogPostRepository blogPostRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public BlogPost approve(UUID postId, UUID moderatorId){
        BlogPost post = getPost(postId);
        if (post.getStatus() != BlogStatus.PENDING) {
            throw new IllegalStateException("Post already moderated: " + post.getStatus());
        }
        post.setStatus(BlogStatus.APPROVED);
        post.setRejectionReason(null);
        post = blogPostRepository.save(post);

        eventPublisher.publish(new BlogApprovedEvent(this, postId, post.getAuthorId(), moderatorId));
        return post;
    }

    @Transactional
    public BlogPost reject(UUID postId, String reason, UUID moderatorId){
        BlogPost post = getPost(postId);
        if (post.getStatus() != BlogStatus.PENDING) {
            throw new IllegalStateException("Post already moderated: " + post.getStatus());
        }
        post.setStatus(BlogStatus.REJECTED);
        post.setRejectionReason(reason);
        post = blogPostRepository.save(post);

        eventPublisher.publish(new BlogRejectedEvent(this, postId, post.getAuthorId(), moderatorId, reason));
        return post;
    }

    private BlogPost getPost(UUID postId){
        return blogPostRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Blog post not found: " + postId));
    }
}
