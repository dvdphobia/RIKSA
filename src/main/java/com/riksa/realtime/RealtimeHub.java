package com.riksa.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The open app connections on this instance, by learner, and delivery of
 * {@link RealtimeEvent}s to them.
 *
 * <p>Connections live in memory, so with more than one API instance an event
 * only reaches learners connected to the instance that raised it. Relay events
 * over Redis pub/sub before scaling out.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeHub {

    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT = 64 * 1024;

    private final ObjectMapper objectMapper;
    private final Map<UUID, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    void register(UUID userId, WebSocketSession session) {
        // The decorator serializes sends, which may come from any thread.
        sessionsByUser
                .computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet())
                .add(new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT));
    }

    void unregister(UUID userId, WebSocketSession session) {
        sessionsByUser.computeIfPresent(userId, (id, sessions) -> {
            sessions.removeIf(open -> open.getId().equals(session.getId()));
            return sessions.isEmpty() ? null : sessions;
        });
    }

    /** Sends right away. Prefer publishing a {@link RealtimeEvent}, which waits for the commit. */
    public void send(UUID recipientId, String type, Map<String, Object> data) {
        TextMessage message;
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("type", type);
            body.put("data", data == null ? Map.of() : data);
            message = new TextMessage(objectMapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            log.warn("Could not encode realtime event {}", type, e);
            return;
        }
        if (recipientId == null) {
            sessionsByUser.values().forEach(sessions -> deliver(sessions, message));
        } else {
            Set<WebSocketSession> sessions = sessionsByUser.get(recipientId);
            if (sessions != null) deliver(sessions, message);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRealtimeEvent(RealtimeEvent event) {
        send(event.getRecipientId(), event.getType(), event.getData());
    }

    private void deliver(Collection<WebSocketSession> sessions, TextMessage message) {
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) continue;
            try {
                session.sendMessage(message);
            } catch (IOException | RuntimeException e) {
                // A slow or dropped connection must not fail the request that
                // raised the event; the app reconnects and refetches.
                log.debug("Dropping realtime event for session {}", session.getId(), e);
            }
        }
    }
}
