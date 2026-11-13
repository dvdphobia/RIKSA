package com.riksa.realtime;

import com.riksa.auth.security.JwtService;
import com.riksa.auth.security.UserSecurityState;
import com.riksa.auth.security.UserSecurityStateService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeHandshakeInterceptorTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserSecurityStateService states = mock(UserSecurityStateService.class);
    private final RealtimeHandshakeInterceptor interceptor = new RealtimeHandshakeInterceptor(jwtService, states);

    @Test
    void rejectsAConnectionWithoutAToken() {
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        Map<String, Object> attributes = new HashMap<>();

        boolean admitted = interceptor.beforeHandshake(request(RealtimeConfig.PATH), response, null, attributes);

        assertThat(admitted).isFalse();
        assertThat(attributes).isEmpty();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsAnInvalidToken() {
        when(jwtService.isAccessTokenValid("bad")).thenReturn(false);
        ServerHttpResponse response = mock(ServerHttpResponse.class);

        boolean admitted = interceptor.beforeHandshake(
                request(RealtimeConfig.PATH + "?token=bad"), response, null, new HashMap<>());

        assertThat(admitted).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsARefreshTokenEvenWhenItsSignatureIsValid() {
        when(jwtService.isAccessTokenValid("refresh")).thenReturn(false);
        ServerHttpResponse response = mock(ServerHttpResponse.class);

        boolean admitted = interceptor.beforeHandshake(
                request(RealtimeConfig.PATH + "?token=refresh"), response, null, new HashMap<>());

        assertThat(admitted).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(jwtService, org.mockito.Mockito.never()).getUserId("refresh");
    }

    @Test
    void rejectsADisabledAccount() {
        UUID userId = UUID.randomUUID();
        UserSecurityState state = new UserSecurityState(false, "USER");
        when(jwtService.isAccessTokenValid("good")).thenReturn(true);
        when(jwtService.getUserId("good")).thenReturn(userId);
        when(states.get(userId)).thenReturn(state);
        ServerHttpResponse response = mock(ServerHttpResponse.class);

        boolean admitted = interceptor.beforeHandshake(
                request(RealtimeConfig.PATH + "?token=good"), response, null, new HashMap<>());

        assertThat(admitted).isFalse();
        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
    }

    @Test
    void admitsAnActiveAccountAndRemembersWhoItIs() {
        UUID userId = UUID.randomUUID();
        UserSecurityState state = new UserSecurityState(true, "USER");
        when(jwtService.isAccessTokenValid("good")).thenReturn(true);
        when(jwtService.getUserId("good")).thenReturn(userId);
        when(states.get(userId)).thenReturn(state);
        Map<String, Object> attributes = new HashMap<>();

        boolean admitted = interceptor.beforeHandshake(
                request(RealtimeConfig.PATH + "?token=good"), mock(ServerHttpResponse.class), null, attributes);

        assertThat(admitted).isTrue();
        assertThat(attributes).containsEntry(RealtimeHandshakeInterceptor.USER_ID, userId);
    }

    private static ServerHttpRequest request(String pathAndQuery) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("http://localhost" + pathAndQuery));
        return request;
    }
}
