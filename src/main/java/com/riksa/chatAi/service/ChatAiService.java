package com.riksa.chatAi.service;

import com.riksa.auth.entity.User;
import com.riksa.auth.repository.UserRepository;
import com.riksa.chatAi.dto.*;
import com.riksa.chatAi.entity.ChatMessage;
import com.riksa.chatAi.entity.ChatSession;
import com.riksa.chatAi.repository.ChatMessageRepository;
import com.riksa.chatAi.repository.ChatSessionRepository;
import com.riksa.common.exception.ResourceNotFoundException;
import com.riksa.common.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatAiService {
    private static final String SYSTEM_PROMPT = """
            You are Riksa AI Tutor, a patient bilingual learning coach for Cambodian students.
            Explain concepts step by step, ask a short checking question, and match the learner's
            language (Khmer or English). Teach reasoning instead of giving unsupported final answers.
            Never claim to have performed actions outside this conversation.
            """;
    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final UserRepository users;
    @Value("${openrouter.api-key:}") private String apiKey;
    @Value("${openrouter.model:openrouter/auto}") private String model;
    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}") private String baseUrl;

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> list(UUID userId) {
        return sessions.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(session -> response(session, List.of())).toList();
    }

    @Transactional(readOnly = true)
    public ChatSessionResponse get(UUID userId, UUID id) {
        ChatSession session = owned(userId, id);
        var history = messages.findBySessionIdOrderByCreatedAtAsc(id).stream()
                .map(this::response).toList();
        return response(session, history);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        sessions.delete(owned(userId, id));
    }

    @Transactional
    public ChatTurnResponse send(UUID userId, SendChatMessageRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ServiceUnavailableException("AI tutor is not configured", null);
        }
        ChatSession session = request.sessionId() == null
                ? create(userId, request.learningContext()) : owned(userId, request.sessionId());
        if (request.learningContext() != null && !request.learningContext().isBlank()) {
            session.setContextSummary(request.learningContext().trim());
        }
        ChatMessage learner = new ChatMessage();
        learner.setSession(session);
        learner.setRole("user");
        learner.setContent(request.message().trim());
        messages.save(learner);

        var recent = new ArrayList<>(messages.findTop20BySessionIdOrderByCreatedAtDesc(session.getId()));
        Collections.reverse(recent);
        String answer = complete(session, recent);
        ChatMessage tutor = new ChatMessage();
        tutor.setSession(session);
        tutor.setRole("assistant");
        tutor.setContent(answer);
        messages.save(tutor);
        session.setUpdatedAt(OffsetDateTime.now());
        sessions.save(session);
        return new ChatTurnResponse(session.getId(), response(tutor));
    }

    private ChatSession create(UUID userId, String context) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setContextSummary(context == null || context.isBlank() ? null : context.trim());
        return sessions.save(session);
    }

    private ChatSession owned(UUID userId, UUID id) {
        return sessions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));
    }

    private String complete(ChatSession session, List<ChatMessage> history) {
        List<Map<String, String>> prompt = new ArrayList<>();
        String context = session.getContextSummary();
        prompt.add(Map.of("role", "system", "content", context == null
                ? SYSTEM_PROMPT : SYSTEM_PROMPT + "\nLearning context: " + context));
        history.forEach(item -> prompt.add(Map.of("role", item.getRole(), "content", item.getContent())));
        try {
            Map<?, ?> body = RestClient.create(baseUrl).post().uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header("HTTP-Referer", "https://riksa.org")
                    .header("X-Title", "Riksa AI Tutor")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", model, "messages", prompt,
                            "temperature", 0.35, "max_tokens", 900))
                    .retrieve().body(Map.class);
            String content = assistantContent(body);
            if (content == null || content.isBlank()) throw new IllegalStateException("Empty AI response");
            return content.trim();
        } catch (Exception error) {
            org.slf4j.LoggerFactory.getLogger(ChatAiService.class)
                    .error("OpenRouter AI request failed (model: {}, baseUrl: {}): {}", model, baseUrl, error.getMessage(), error);
            throw new ServiceUnavailableException("AI provider request failed: " + error.getMessage(), error);
        }
    }

    private String assistantContent(Map<?, ?> body) {
        if (body == null || !(body.get("choices") instanceof List<?> choices) || choices.isEmpty()
                || !(choices.getFirst() instanceof Map<?, ?> choice)
                || !(choice.get("message") instanceof Map<?, ?> message)) {
            return null;
        }
        Object content = message.get("content");
        return content instanceof String value ? value : null;
    }

    private ChatSessionResponse response(ChatSession session, List<ChatMessageResponse> history) {
        String title = session.getContextSummary();
        if (title == null || title.isBlank()) title = "New learning chat";
        if (title.length() > 80) title = title.substring(0, 80);
        return new ChatSessionResponse(session.getId(), title, session.getStartedAt(), session.getUpdatedAt(), history);
    }

    private ChatMessageResponse response(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
    }
}
