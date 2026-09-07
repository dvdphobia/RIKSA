package com.riksa.chatAi.dto;

import java.util.UUID;
public record ChatTurnResponse(UUID sessionId, ChatMessageResponse message) {}
