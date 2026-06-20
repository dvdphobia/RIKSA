package com.riksa.blog.dto;

import java.util.UUID;

public record  BlogPostRequest (
    String title,
    String content,
    String coverUrl,
    UUID categoryId
){}
