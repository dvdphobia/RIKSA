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
        RateLimitFilter filter = new RateLimitFilter(5, 60);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/lessons");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void authPath_withinLimit_passesThrough() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(5, 60);
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
        RateLimitFilter filter = new RateLimitFilter(2, 60);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/auth/login");
            response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, chain);
        }

        assertThat(response.getStatus()).isEqualTo(429);
    }
}
