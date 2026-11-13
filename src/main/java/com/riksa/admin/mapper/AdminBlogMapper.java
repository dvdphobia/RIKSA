package com.riksa.admin.mapper;

import com.riksa.admin.dto.AdminBlogPostResponse;
import com.riksa.blog.entity.BlogPost;

public final class AdminBlogMapper {

    private AdminBlogMapper() {
    }

    public static AdminBlogPostResponse toResponse(BlogPost post, boolean liked) {
        return new AdminBlogPostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getTitle(),
                post.getContent(),
                post.getCoverUrl(),
                post.getStatus(),
                post.getRejectionReason(),
                post.getHiddenReason(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCategoryId(),
                liked,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getModerationScore(),
                post.getModerationReasons()
        );
    }
}
