package com.riksa.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_BASE = "/api/v1/auth";

    private final RedisRateLimiter rateLimiter;
    private final boolean trustForwardHeaders;
    private final Map<String, Policy> policies;

    public RateLimitFilter(
            RedisRateLimiter rateLimiter,
            @Value("${rate-limit.auth.max-requests:5}") int maxRequests,
            @Value("${rate-limit.auth.window-seconds:60}") long windowSeconds,
            @Value("${rate-limit.trust-forward-headers:false}") boolean trustForwardHeaders
    ) {
        this.rateLimiter = rateLimiter;
        this.trustForwardHeaders = trustForwardHeaders;
        this.policies = Map.ofEntries(
                Map.entry("/login", new Policy("login", maxRequests, Duration.ofSeconds(windowSeconds))),
                Map.entry("/register", new Policy("register", 5, Duration.ofHours(1))),
                Map.entry("/forgot-password", new Policy("forgot-password", 5, Duration.ofMinutes(15))),
                Map.entry("/reset-password", new Policy("reset-password", 10, Duration.ofMinutes(15))),
                Map.entry("/otp/send", new Policy("otp-send", 10, Duration.ofHours(1))),
                Map.entry("/otp/verify", new Policy("otp-verify", 20, Duration.ofMinutes(15))),
                Map.entry("/telegram/start", new Policy("telegram-start", 10, Duration.ofMinutes(10))),
                Map.entry("/telegram/status", new Policy("telegram-status", 60, Duration.ofMinutes(1))),
                // Codes are 8 chars from a 32-symbol alphabet; this keeps guessing hopeless.
                Map.entry("/telegram/code", new Policy("telegram-code", 10, Duration.ofMinutes(15))),
                Map.entry("/telegram/complete", new Policy("telegram-complete", 20, Duration.ofMinutes(15))),
                Map.entry("/telegram", new Policy("telegram", 10, Duration.ofMinutes(1))),
                Map.entry("/google", new Policy("google", 10, Duration.ofMinutes(1))),
                Map.entry("/refresh", new Policy("refresh", 30, Duration.ofMinutes(1))),
                Map.entry("/verify-email", new Policy("verify-email", 20, Duration.ofHours(1))),
                Map.entry("/resend-verification-email", new Policy("resend-verification-email", 5, Duration.ofMinutes(15)))
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || (AUTH_BASE + "/telegram/webhook").equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String route = route(request.getRequestURI());
        Policy policy = route == null ? null : policies.get(route);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            RateLimitDecision decision = rateLimiter.acquire(
                    policy.bucket(), clientIp(request), policy.limit(), policy.window());
            response.setHeader("X-RateLimit-Limit", Long.toString(decision.limit()));
            response.setHeader("X-RateLimit-Remaining", Long.toString(decision.remaining()));
            if (!decision.allowed()) {
                response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
                writeError(response, HttpStatus.TOO_MANY_REQUESTS,
                        "Rate limit exceeded. Try again later.");
                return;
            }
        } catch (RuntimeException e) {
            log.error("Redis rate limiter unavailable for route {}", route, e);
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE,
                    "Authentication service is temporarily unavailable. Try again later.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String route(String uri) {
        if (uri == null || !uri.startsWith(AUTH_BASE)) return null;
        String value = uri.substring(AUTH_BASE.length());
        return value.isBlank() ? "/" : value;
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message + "\"}");
    }

    private record Policy(String bucket, long limit, Duration window) {}
}
