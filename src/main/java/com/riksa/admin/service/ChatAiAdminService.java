package com.riksa.admin.service;

import com.riksa.admin.dto.ChatSessionDetailResponse;
import com.riksa.admin.dto.ChatSessionSummaryResponse;
import com.riksa.admin.dto.ChatUsageResponse;
import com.riksa.auth.repository.UserRepository;
import com.riksa.chatAi.entity.ChatMessage;
import com.riksa.chatAi.entity.ChatSession;
import com.riksa.chatAi.repository.ChatMessageRepository;
import com.riksa.chatAi.repository.ChatSessionRepository;
import com.riksa.common.audit.AuditLogService;
import com.riksa.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatAiAdminService {

    private static final int TOP_USER_LIMIT = 10;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final SettingsService settingsService;

    @Value("${openrouter.api-key:}")
    private String openRouterApiKey;

    @Transactional(readOnly = true)
    public ChatUsageResponse usage(int days) {
        int safeDays = Math.max(1, Math.min(days, 365));
        OffsetDateTime since = OffsetDateTime.now().minusDays(safeDays);
        List<ChatMessageRepository.UserUsageRow> rows = chatMessageRepository.findUsageByCreatedAtAfter(since);
        long totalMessages = rows.stream().mapToLong(ChatMessageRepository.UserUsageRow::getMessageCount).sum();
        List<ChatUsageResponse.TopUser> topUsers = rows.stream()
                .sorted(Comparator.comparingLong(ChatMessageRepository.UserUsageRow::getMessageCount).reversed()
                        .thenComparing(ChatMessageRepository.UserUsageRow::getLastMessageAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(TOP_USER_LIMIT)
                .map(row -> new ChatUsageResponse.TopUser(row.getUserId(), row.getUsername(),
                        row.getDisplayName(), row.getMessageCount(), row.getSessionCount(), row.getLastMessageAt()))
                .toList();

        var settings = settingsService.getAiChat();
        boolean enabled = settings.enabled() && openRouterApiKey != null && !openRouterApiKey.isBlank();
        return new ChatUsageResponse(safeDays, totalMessages, rows.size(), settings.dailyMessageLimit(), enabled,
                topUsers);
    }

    @Transactional(readOnly = true)
    public List<ChatSessionSummaryResponse> sessions(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return chatSessionRepository.findSessionSummariesByUserId(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatSessionDetailResponse transcript(UUID actorId, UUID sessionId, String reason) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found: " + sessionId));
        List<ChatMessage> messages = chatMessageRepository
                .findBySessionIdOrderByCreatedAtAscIdAsc(sessionId);
        ChatSessionSummaryResponse summary = new ChatSessionSummaryResponse(
                session.getId(), session.getUser().getId(), session.getContextSummary(), messages.size(),
                session.getStartedAt(), session.getUpdatedAt());
        List<ChatSessionDetailResponse.Message> transcript = messages.stream()
                .map(this::toMessage)
                .toList();

        auditLogService.record(actorId, "AI_CHAT_TRANSCRIPT_VIEW", "CHAT_SESSION", sessionId,
                Map.of("userId", session.getUser().getId().toString(), "reason", reason));
        return new ChatSessionDetailResponse(summary, transcript);
    }

    private ChatSessionSummaryResponse toSummary(ChatSessionRepository.SessionSummaryRow row) {
        return new ChatSessionSummaryResponse(row.getId(), row.getUserId(), row.getContextSummary(),
                row.getMessageCount(), row.getStartedAt(), row.getUpdatedAt());
    }

    private ChatSessionDetailResponse.Message toMessage(ChatMessage message) {
        String role = "user".equalsIgnoreCase(message.getRole()) ? "user" : "assistant";
        return new ChatSessionDetailResponse.Message(message.getId(), role, message.getContent(),
                message.getCreatedAt());
    }
}
