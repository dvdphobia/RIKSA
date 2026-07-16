package com.riksa.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record  BlogPostRequest (
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    String title,

    @NotBlank(message = "Content is required")
    String content,

    @Size(max = 2000, message = "Cover URL must be at most 2000 characters")
    String coverUrl,

    UUID categoryId
){}
