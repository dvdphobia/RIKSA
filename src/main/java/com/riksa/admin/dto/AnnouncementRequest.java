package com.riksa.admin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record AnnouncementRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String body,
        @NotNull OffsetDateTime startsAt,
        OffsetDateTime endsAt) {

    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isWindowValid() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
}
