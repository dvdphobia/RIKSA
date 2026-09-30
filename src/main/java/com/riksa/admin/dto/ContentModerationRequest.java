package com.riksa.admin.dto;

import com.riksa.social.enums.ContentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentModerationRequest {
    @NotNull
    private ContentStatus status;

    private String reason;
}
