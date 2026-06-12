package com.riksa.blog.service;

import com.riksa.blog.entity.BlogLike;
import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.entity.BlogView;
import com.riksa.blog.repository.BlogLikeRepository;
import com.riksa.blog.repository.BlogPostRepository;
import com.riksa.blog.repository.BlogViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlogEngagementService {

    private final BlogPostRepository blogPostRepository;
    private final BlogViewRepository blogViewRepository;
    private final BlogLikeRepository blogLikeRepository;

    @Transactional
    public BlogPost trackView(UUID blogPostId, UUID userId) {
        BlogView view = new BlogView();
        view.setBlogPostId(blogPostId);
        view.setUserId(userId); // nullable — guest views pass null
        blogViewRepository.save(view);

        BlogPost post = getPost(blogPostId);
        post.setViewCount(post.getViewCount() + 1);
        return blogPostRepository.save(post);
    }

    @Transactional
    public BlogPost toggleLike(UUID blogPostId, UUID userId) {
        BlogPost post = getPost(blogPostId);
        Optional<BlogLike> existing = blogLikeRepository.findByUserIdAndBlogPostid(userId, blogPostId);

        if (existing.isPresent()) {
            // unlike
            blogLikeRepository.delete(existing.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else {
            // like
            BlogLike like = new BlogLike();
            like.setUserId(userId);
            like.setBlogPostId(blogPostId);
            blogLikeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
        }

        return blogPostRepository.save(post);
    }

    public boolean isLiked(UUID blogPostId, UUID userId) {
        return blogLikeRepository.existByUserIdAndBlogPostId(userId, blogPostId);
    }

    private BlogPost getPost(UUID blogPostId) {
        return blogPostRepository.findById(blogPostId)
                .orElseThrow(() -> new IllegalArgumentException("Blog post not found: " + blogPostId));
    }
}