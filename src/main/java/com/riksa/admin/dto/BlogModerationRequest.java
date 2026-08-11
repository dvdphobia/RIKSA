package com.riksa.admin.dto;

import com.riksa.blog.enums.BlogStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlogModerationRequest {
    @NotNull
    private BlogStatus status;

    private String reason;
}
