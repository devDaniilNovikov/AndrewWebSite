package ru.andrew.website.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.DispatcherType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.util.matcher.RequestMatcher;
import tools.jackson.databind.json.JsonMapper;

class WebFilterBoundaryTest {
    private final ProblemResponseWriter problems =
            new ProblemResponseWriter(JsonMapper.builder().build());

    @Test
    void rateLimiterIgnoresAPostOutsideTheExactLeadPath() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(
                properties(true),
                ClientRateLimiter.defaults(clock()),
                problems);
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
                problems);
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

    @Test
    void exactClosedNamespaceRootsAreDeniedBeforeReachingMvc() throws Exception {
        PublicBoundaryDenyFilter filter =
                new PublicBoundaryDenyFilter(new MockEnvironment());

        for (String path : List.of("/api", "/actuator", "/error")) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(
                    new MockHttpServletRequest("GET", path),
                    response,
                    (ignoredRequest, ignoredResponse) -> {
                        throw new AssertionError(path + " must not reach the chain");
                    });

            assertThat(response.getStatus()).as(path).isEqualTo(403);
        }
    }

    @Test
    void namespaceMatcherDistinguishesRootChildAndNearCollision() {
        RequestMatcher matcher = invokeMatcher("namespace", "/api");

        assertThat(matcher.matches(new MockHttpServletRequest("GET", "/api"))).isTrue();
        assertThat(matcher.matches(new MockHttpServletRequest("GET", "/api/leads"))).isTrue();
        assertThat(matcher.matches(new MockHttpServletRequest("GET", "/apiary"))).isFalse();
    }

    @Test
    void errorDispatchMatcherRequiresBothDispatcherTypeAndExactPath() {
        RequestMatcher matcher = invokeMatcher("errorDispatch", "/error");
        MockHttpServletRequest wrongPath = new MockHttpServletRequest("GET", "/different");
        wrongPath.setDispatcherType(DispatcherType.ERROR);

        assertThat(matcher.matches(wrongPath)).isFalse();
    }

    private static RequestMatcher invokeMatcher(String methodName, String value) {
        try {
            Method method = SecurityConfiguration.class.getDeclaredMethod(methodName, String.class);
            method.setAccessible(true);
            return (RequestMatcher) method.invoke(null, value);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new AssertionError(exception);
        } catch (InvocationTargetException exception) {
            throw new AssertionError(exception.getCause());
        }
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
