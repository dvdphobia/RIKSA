package com.riksa.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

/** Tracks each app connection; the app only listens, apart from keep-alive pings. */
@Component
@RequiredArgsConstructor
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final RealtimeHub hub;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID userId = userId(session);
        if (userId != null) hub.register(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Pings from the app keep proxies from closing an idle connection.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = userId(session);
        if (userId != null) hub.unregister(userId, session);
    }

    private static UUID userId(WebSocketSession session) {
        return session.getAttributes().get(RealtimeHandshakeInterceptor.USER_ID) instanceof UUID id ? id : null;
    }
}
