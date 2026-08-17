package ru.andrew.website.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(Ordered.HIGHEST_PRECEDENCE)
final class PerimeterRateLimitFilter extends OncePerRequestFilter {
    private static final long MAX_RETRY_AFTER_SECONDS = 3_600L;
    private static final String FIXED_INSTANCE = "/";

    private final boolean enabled;
    private final SlidingWindowRateLimiter limiter;
    private final ProblemResponseWriter problems;

    PerimeterRateLimitFilter(
            boolean enabled,
            SlidingWindowRateLimiter limiter,
            ProblemResponseWriter problems) {
        this.enabled = enabled;
        this.limiter = limiter;
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled
                || request.getLocalPort()
                        == ProductionHttpInvariantGuard.MANAGEMENT_SERVER_PORT
                && "127.0.0.1".equals(request.getLocalAddr());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (limiter.tryAcquire()) {
            chain.doFilter(request, response);
            return;
        }

        response.setHeader(
                HttpHeaders.RETRY_AFTER,
                Long.toString(retryAfterSeconds(limiter.retryAfter())));
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        problems.write(response, problems.problem(
                HttpStatus.TOO_MANY_REQUESTS,
                "urn:andrew:problem:perimeter-rate-limit-exceeded",
                "Too many requests",
                "Wait before retrying.",
                FIXED_INSTANCE));
    }

    private long retryAfterSeconds(Duration retryAfter) {
        long seconds = Math.max(1L, Math.ceilDiv(retryAfter.toMillis(), 1_000L));
        return Math.min(MAX_RETRY_AFTER_SECONDS, seconds);
    }
}
