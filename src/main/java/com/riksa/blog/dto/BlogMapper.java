package com.riksa.blog.dto;

import com.riksa.blog.entity.BlogPost;

public class BlogMapper {

    public static BlogPostResponse toResponse(BlogPost post, boolean liked) {
        return new BlogPostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getTitle(),
                post.getContent(),
                post.getCoverUrl(),
                post.getStatus(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCategoryId(),
                liked,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}