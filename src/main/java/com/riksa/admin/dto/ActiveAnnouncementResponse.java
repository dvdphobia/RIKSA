package com.riksa.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ActiveAnnouncementResponse(
        UUID id,
        String title,
        String body,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt) {
}
