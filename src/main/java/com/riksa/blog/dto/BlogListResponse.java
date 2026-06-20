package com.riksa.blog.dto;

import java.util.List;

public record BlogListResponse(
        List<BlogPostResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}