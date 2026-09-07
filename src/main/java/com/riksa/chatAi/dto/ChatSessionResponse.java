package com.riksa.chatAi.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ChatSessionResponse(UUID id, String title, OffsetDateTime startedAt,
                                  OffsetDateTime updatedAt, List<ChatMessageResponse> messages) {}
