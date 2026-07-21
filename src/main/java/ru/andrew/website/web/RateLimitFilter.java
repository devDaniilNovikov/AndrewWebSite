package ru.andrew.website.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

final class RateLimitFilter extends OncePerRequestFilter {
    private static final String LEAD_PATH = "/api/leads";
    private static final long MAX_RETRY_AFTER_SECONDS = 3_600L;

    private final boolean enabled;
    private final ClientRateLimiter limiter;
    private final ProblemResponseWriter problems;

    RateLimitFilter(WebProperties properties, ClientRateLimiter limiter,
            ProblemResponseWriter problems) {
        this.enabled = properties.rateLimit().enabled();
        this.limiter = limiter;
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled
                || !HttpMethod.POST.matches(request.getMethod())
                || !LEAD_PATH.equals(path(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String connectionAddress = request.getRemoteAddr();
        RateDecision decision = limiter.tryAcquire(connectionAddress == null ? "" : connectionAddress);
        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }

        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds(decision.retryAfter())));
        problems.write(response, problems.problem(
                HttpStatus.TOO_MANY_REQUESTS,
                "urn:andrew:problem:rate-limit-exceeded",
                "Too many requests",
                "Wait before submitting another request.",
                path(request)));
    }

    private long retryAfterSeconds(Duration retryAfter) {
        long seconds = Math.max(1L, Math.ceilDiv(retryAfter.toMillis(), 1_000L));
        return Math.min(MAX_RETRY_AFTER_SECONDS, seconds);
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
