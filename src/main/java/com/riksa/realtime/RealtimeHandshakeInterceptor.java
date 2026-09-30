package com.riksa.realtime;

import com.riksa.auth.security.JwtService;
import com.riksa.auth.security.UserSecurityState;
import com.riksa.auth.security.UserSecurityStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

/**
 * Admits a connection only with a valid access token for an active account.
 *
 * <p>Browsers cannot set headers on a WebSocket, so the token comes as the
 * {@code token} query parameter.
 */
@Component
@RequiredArgsConstructor
public class RealtimeHandshakeInterceptor implements HandshakeInterceptor {

    static final String USER_ID = "riksa.userId";

    private final JwtService jwtService;
    private final UserSecurityStateService userSecurityStateService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (token == null || token.isBlank() || !jwtService.isTokenValid(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        UUID userId = jwtService.getUserId(token);
        UserSecurityState state = userSecurityStateService.get(userId);
        if (!state.active()) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        attributes.put(USER_ID, userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
