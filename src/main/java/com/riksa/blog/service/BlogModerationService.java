package com.riksa.blog.service;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.repository.BlogPostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlogModerationService {
    private final BlogPostRepository blogPostRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BlogPost approve(UUID postId){
        BlogPost post = getPost(postId);
        if (post.getStatus() != BlogStatus.PENDING) {
            throw new IllegalStateException("Post already moderated: " + post.getStatus());
        }
        post.setStatus(BlogStatus.APPROVED);
        post.setRejectionReason(null);
        return blogPostRepository.save(post);
    }
    @Transactional
    public BlogPost reject(UUID postId, String reason){
        BlogPost post = getPost(postId);
        if (post.getStatus() != BlogStatus.PENDING) {
            throw new IllegalStateException("Post already moderated: " + post.getStatus());
        }
        post.setStatus(BlogStatus.REJECTED);
        post.setRejectionReason(reason);
        return blogPostRepository.save(post);
    }
    private BlogPost getPost(UUID postId){
        return blogPostRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Blog post not found: " + postId));
    }
}
