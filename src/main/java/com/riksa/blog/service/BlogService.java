package com.riksa.blog.service;

import com.riksa.blog.entity.BlogPost;
import com.riksa.blog.enums.BlogStatus;
import com.riksa.blog.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.riksa.blog.util.BadWordFilter;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogPostRepository blogPostRepository;
    private final BadWordFilter badWordFilter;

    @Transactional
    public BlogPost createPost(UUID authorId, String title, String content, String coverUrl, UUID categoryId) {
        badWordFilter.validate(title + " " + content);

        BlogPost post = new BlogPost();
        post.setAuthorId(authorId);
        post.setTitle(title);
        post.setContent(content);
        post.setCoverUrl(coverUrl);
        post.setCategoryId(categoryId);
        post.setStatus(BlogStatus.PENDING);

        return blogPostRepository.save(post);
    }

    public Page<BlogPost> listPosts(BlogStatus status, UUID categoryId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (categoryId != null) {
            return blogPostRepository.findByStatusAndCategoryId(status, categoryId, pageable);
        }
        return blogPostRepository.findByStatus(status, pageable);
    }

    public BlogPost getPost(UUID postId) {
        return blogPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Blog post not found: " + postId));
    }

    public Page<BlogPost> getMyPosts(UUID authorId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return blogPostRepository.findByAuthorId(authorId, pageable);
    }
}