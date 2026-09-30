package com.riksa.admin.dto;

import java.util.List;

public record AdminBlogListResponse(
        List<AdminBlogPostResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
