package ru.andrew.website.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ru.andrew.website.leads.LeadMetrics;
import ru.andrew.website.leads.LeadRejectionReason;
import tools.jackson.databind.json.JsonMapper;

class LeadRejectionTelemetryTest {
    @Test
    void adviceMapsEveryHandledFailureToItsCanonicalReason() {
        var registry = new SimpleMeterRegistry();
        var metrics = new LeadMetrics(registry);
        var advice = new ProblemResponseAdvice(writer(), metrics);
        var request =
                new MockHttpServletRequest("POST", "/api/leads");

        advice.malformedPayload(request);
        advice.invalidRequest(request);
        advice.idempotencyConflict(request);
        advice.serviceUnavailable(request);
        advice.unsupportedMediaType(request);

        assertReason(registry, LeadRejectionReason.PAYLOAD);
        assertReason(registry, LeadRejectionReason.VALIDATION);
        assertReason(registry, LeadRejectionReason.CONFLICT);
        assertReason(registry, LeadRejectionReason.UNAVAILABLE);
        assertReason(registry, LeadRejectionReason.MEDIA_TYPE);
    }

    @Test
    void requestBoundaryRecordsOversizedAndUnsupportedBodies() throws Exception {
        var registry = new SimpleMeterRegistry();
        var metrics = new LeadMetrics(registry);
        var filter = new RequestBodyLimitFilter(
                properties(), writer(), metrics);

        MockHttpServletRequest oversized =
                new MockHttpServletRequest("POST", "/api/leads");
        oversized.setContentType("application/json");
        oversized.setContent("012345678".getBytes());
        filter.doFilter(
                oversized,
                new MockHttpServletResponse(),
                (request, response) -> {
                    throw new AssertionError("oversized body reached chain");
                });

        MockHttpServletRequest unsupported =
                new MockHttpServletRequest("POST", "/api/leads");
        unsupported.setContent("{}".getBytes());
        filter.doFilter(
                unsupported,
                new MockHttpServletResponse(),
                (request, response) -> {
                    throw new AssertionError("unsupported body reached chain");
                });

        assertReason(registry, LeadRejectionReason.PAYLOAD);
        assertReason(registry, LeadRejectionReason.MEDIA_TYPE);
    }

    @Test
    void rateLimitBoundaryRecordsOnlyTheBoundedReason() throws Exception {
        var registry = new SimpleMeterRegistry();
        var metrics = new LeadMetrics(registry);
        ClientRateLimiter limiter = mock(ClientRateLimiter.class);
        when(limiter.tryAcquire("127.0.0.1"))
                .thenReturn(RateDecision.rejected(Duration.ofSeconds(1)));
        var filter = new RateLimitFilter(
                properties(), limiter, writer(), metrics);
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/leads");

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> {
                    throw new AssertionError("limited request reached chain");
                });

        assertReason(registry, LeadRejectionReason.RATE_LIMIT);
    }

    private static ProblemResponseWriter writer() {
        return new ProblemResponseWriter(JsonMapper.builder().build());
    }

    private static WebProperties properties() {
        return new WebProperties(
                8,
                new WebProperties.RateLimit(
                        true,
                        10,
                        Duration.ofHours(1),
                        10,
                        Duration.ofMinutes(1),
                        1,
                        Duration.ofMinutes(1)),
                List.of());
    }

    private static void assertReason(
            SimpleMeterRegistry registry, LeadRejectionReason reason) {
        assertThat(registry.find("andrew.leads.rejected")
                        .tag("reason", reason.metricValue())
                        .counter())
                .satisfies(counter ->
                        assertThat(counter.count()).isEqualTo(1.0));
    }
}
