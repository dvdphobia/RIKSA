package com.riksa.chatAi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SendChatMessageRequest(UUID sessionId,
                                     @NotBlank @Size(max = 4000) String message,
                                     @Size(max = 2000) String learningContext) {}
