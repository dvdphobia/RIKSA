package com.riksa.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    @Test
    void nonAuthPath_passesThrough() throws Exception {
        RedisRateLimiter limiter = mock(RedisRateLimiter.class);
        RateLimitFilter filter = new RateLimitFilter(limiter, 5, 60, false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/lessons");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(limiter);
    }

    @Test
    void authPath_withinLimit_passesThrough() throws Exception {
        RedisRateLimiter limiter = mock(RedisRateLimiter.class);
        when(limiter.acquire(eq("login"), anyString(), eq(5L), any()))
                .thenReturn(new RateLimitDecision(true, 5, 4, 60));
        RateLimitFilter filter = new RateLimitFilter(limiter, 5, 60, false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void authPath_exceedsLimit_returns429() throws Exception {
        RedisRateLimiter limiter = mock(RedisRateLimiter.class);
        when(limiter.acquire(eq("login"), anyString(), eq(2L), any()))
                .thenReturn(new RateLimitDecision(false, 2, 0, 42));
        RateLimitFilter filter = new RateLimitFilter(limiter, 2, 60, false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("42");
        verifyNoInteractions(chain);
    }

    @Test
    void telegramWebhook_bypassesRateLimit() throws Exception {
        RedisRateLimiter limiter = mock(RedisRateLimiter.class);
        RateLimitFilter filter = new RateLimitFilter(limiter, 5, 60, false);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/telegram/webhook");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(limiter);
    }

    @Test
    void redisFailure_failsClosedWith503() throws Exception {
        RedisRateLimiter limiter = mock(RedisRateLimiter.class);
        when(limiter.acquire(anyString(), anyString(), anyLong(), any()))
                .thenThrow(new IllegalStateException("redis unavailable"));
        RateLimitFilter filter = new RateLimitFilter(limiter, 5, 60, false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        verifyNoInteractions(chain);
    }
}
