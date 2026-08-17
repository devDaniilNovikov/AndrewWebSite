package ru.andrew.website.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ru.andrew.website.leads.LeadMetrics;
import ru.andrew.website.observability.HealthCacheControlFilter;
import ru.andrew.website.testing.MutableClock;
import tools.jackson.databind.json.JsonMapper;

class PerimeterRateLimitFilterTest {
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    private final ProblemResponseWriter problems =
            new ProblemResponseWriter(JsonMapper.builder().build());

    @Test
    void invalidMediaTypeConsumesTheCoarseAdmissionBeforeBodyValidation()
            throws Exception {
        var filter = filter(1);
        var bodyFilter = new RequestBodyLimitFilter(
                properties(),
                problems,
                new LeadMetrics(new SimpleMeterRegistry()));

        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(
                publicRequest("POST", "/api/leads"),
                first,
                (request, response) -> bodyFilter.doFilter(
                        request,
                        response,
                        (ignoredRequest, ignoredResponse) -> {
                            throw new AssertionError("invalid media type reached the application");
                        }));

        assertThat(first.getStatus()).isEqualTo(415);

        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(
                publicRequest("POST", "/api/leads"),
                second,
                (ignoredRequest, ignoredResponse) -> {
                    throw new AssertionError("body validation ran after perimeter exhaustion");
                });

        assertThat(second.getStatus()).isEqualTo(429);
        assertThat(second.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
        assertThat(second.getContentType())
                .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(second.getContentAsString())
                .contains("\"instance\":\"/\"")
                .doesNotContain("api/leads", "192.0.2.44");
        assertThat(second.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(second.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(second.getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
    }

    @Test
    void everyPublicMethodAndPathSharesTheCoarsePerimeterAdmission() throws Exception {
        var filter = filter(4);
        AtomicInteger downstream = new AtomicInteger();

        for (MockHttpServletRequest request : new MockHttpServletRequest[] {
                publicRequest("GET", "/"),
                publicRequest("HEAD", "/_next/static/app.js"),
                publicRequest("OPTIONS", "/api/leads"),
                publicRequest("DELETE", "/actuator/health/readiness")
        }) {
            filter.doFilter(
                    request,
                    new MockHttpServletResponse(),
                    (ignoredRequest, ignoredResponse) -> downstream.incrementAndGet());
        }

        MockHttpServletResponse rejected = new MockHttpServletResponse();
        filter.doFilter(
                publicRequest("TRACE", "/unmapped"),
                rejected,
                (ignoredRequest, ignoredResponse) -> downstream.incrementAndGet());

        assertThat(downstream).hasValue(4);
        assertThat(rejected.getStatus()).isEqualTo(429);
    }

    @Test
    void privateManagementPortBypassesThePublicLimiterWithoutConsumingIt()
            throws Exception {
        var filter = filter(1);
        AtomicInteger probes = new AtomicInteger();

        for (int index = 0; index < 3; index++) {
            MockHttpServletRequest probe =
                    new MockHttpServletRequest("GET", "/actuator/health/readiness");
            probe.setLocalPort(ProductionHttpInvariantGuard.MANAGEMENT_SERVER_PORT);
            probe.setLocalAddr("127.0.0.1");
            filter.doFilter(
                    probe,
                    new MockHttpServletResponse(),
                    (ignoredRequest, ignoredResponse) -> probes.incrementAndGet());
        }

        MockHttpServletResponse admitted = new MockHttpServletResponse();
        filter.doFilter(
                publicRequest("GET", "/"),
                admitted,
                (ignoredRequest, ignoredResponse) -> probes.incrementAndGet());
        MockHttpServletResponse rejected = new MockHttpServletResponse();
        filter.doFilter(
                publicRequest("GET", "/"),
                rejected,
                (ignoredRequest, ignoredResponse) -> probes.incrementAndGet());

        assertThat(probes).hasValue(4);
        assertThat(admitted.getStatus()).isEqualTo(200);
        assertThat(rejected.getStatus()).isEqualTo(429);
    }

    @Test
    void managementPortBypassAlsoRequiresLoopbackLocalAddress() throws Exception {
        var filter = filter(1);
        MockHttpServletRequest publicRequest =
                new MockHttpServletRequest("GET", "/actuator/health/readiness");
        publicRequest.setLocalPort(ProductionHttpInvariantGuard.MANAGEMENT_SERVER_PORT);
        publicRequest.setLocalAddr("10.0.0.12");

        filter.doFilter(
                publicRequest,
                new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> {});
        MockHttpServletResponse rejected = new MockHttpServletResponse();
        filter.doFilter(
                publicRequest,
                rejected,
                (ignoredRequest, ignoredResponse) -> {
                    throw new AssertionError("non-loopback request bypassed perimeter");
                });

        assertThat(rejected.getStatus()).isEqualTo(429);
    }

    @Test
    void disabledPerimeterBypassesAdmissionEntirely() throws Exception {
        MutableClock clock = new MutableClock(START, ZoneOffset.UTC);
        var filter = new PerimeterRateLimitFilter(
                false,
                new SlidingWindowRateLimiter(1, Duration.ofMinutes(1), clock),
                problems);
        AtomicInteger downstream = new AtomicInteger();

        for (int index = 0; index < 2; index++) {
            filter.doFilter(
                    publicRequest("GET", "/"),
                    new MockHttpServletResponse(),
                    (ignoredRequest, ignoredResponse) -> downstream.incrementAndGet());
        }

        assertThat(downstream).hasValue(2);
    }

    @Test
    void perimeterRunsBeforeEveryHealthAndSecurityBoundary() {
        Order perimeterOrder = AnnotationUtils.findAnnotation(
                PerimeterRateLimitFilter.class, Order.class);
        Order healthOrder = AnnotationUtils.findAnnotation(
                HealthCacheControlFilter.class, Order.class);

        assertThat(perimeterOrder).isNotNull();
        assertThat(perimeterOrder.value()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(healthOrder).isNotNull();
        assertThat(healthOrder.value()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
    }

    private PerimeterRateLimitFilter filter(int limit) {
        MutableClock clock = new MutableClock(START, ZoneOffset.UTC);
        return new PerimeterRateLimitFilter(
                true,
                new SlidingWindowRateLimiter(limit, Duration.ofMinutes(1), clock),
                problems);
    }

    private static MockHttpServletRequest publicRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setLocalPort(ProductionHttpInvariantGuard.PUBLIC_SERVER_PORT);
        request.setRemoteAddr("192.0.2.44");
        return request;
    }

    private static WebProperties properties() {
        return new WebProperties(
                16_384,
                new WebProperties.RateLimit(
                        true,
                        10_000,
                        Duration.ofHours(1),
                        60,
                        Duration.ofMinutes(1),
                        5,
                        Duration.ofMinutes(1)),
                List.of());
    }
}
