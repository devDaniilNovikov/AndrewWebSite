package ru.andrew.website.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ru.andrew.website.leads.LeadMetrics;
import tools.jackson.databind.json.JsonMapper;

class WebFilterBoundaryTest {
    private final ProblemResponseWriter problems =
            new ProblemResponseWriter(JsonMapper.builder().build());

    @Test
    void rateLimiterIgnoresAPostOutsideTheExactLeadPath() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(
                properties(true),
                ClientRateLimiter.defaults(clock()),
                problems,
                new LeadMetrics(new SimpleMeterRegistry()));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/other");
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertThat(invoked).isTrue();
    }

    @Test
    void rateLimiterUsesAnEmptyStableKeyWhenTheConnectionAddressIsUnavailable()
            throws Exception {
        RateLimitFilter filter = new RateLimitFilter(
                properties(true),
                ClientRateLimiter.defaults(clock()),
                problems,
                new LeadMetrics(new SimpleMeterRegistry()));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/leads") {
            @Override
            public String getRemoteAddr() {
                return null;
            }
        };
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertThat(invoked).isTrue();
    }

    private static Clock clock() {
        return Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    }

    private static WebProperties properties(boolean enabled) {
        return new WebProperties(
                16_384,
                new WebProperties.RateLimit(
                        enabled,
                        10_000,
                        Duration.ofHours(1),
                        60,
                        Duration.ofMinutes(1),
                        5,
                        Duration.ofMinutes(1)),
                List.of());
    }
}
