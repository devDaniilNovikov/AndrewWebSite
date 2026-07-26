package ru.andrew.website.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WebPropertiesTest {
    private static final WebProperties.RateLimit CANONICAL_RATE_LIMIT =
            new WebProperties.RateLimit(
                    true,
                    10_000,
                    Duration.ofHours(1),
                    60,
                    Duration.ofMinutes(1),
                    5,
                    Duration.ofMinutes(1));

    @ParameterizedTest
    @MethodSource("invalidLocalOrigins")
    void rejectsOriginsThatAreNotPlainLoopbackHttpOrigins(String origin) {
        assertThatThrownBy(() -> properties(List.of(URI.create(origin))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loopback HTTP origins");
    }

    @ParameterizedTest
    @MethodSource("validLocalOrigins")
    void acceptsExplicitLoopbackHttpOrigins(String origin) {
        assertThat(properties(List.of(URI.create(origin))).localCorsOrigins())
                .containsExactly(URI.create(origin));
    }

    @Test
    void normalizesAMissingLocalOriginListToAnImmutableEmptyList() {
        WebProperties properties = properties(null);

        assertThat(properties.localCorsOrigins()).isEmpty();
        assertThatThrownBy(() -> properties.localCorsOrigins().add(URI.create("http://localhost")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requiresTheRateLimitConfiguration() {
        assertThatThrownBy(() -> new WebProperties(16_384, null, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("rateLimit");
    }

    @Test
    void loopbackCheckDefensivelyRejectsANullOrigin() {
        assertThat(invokeLoopbackCheck(null)).isFalse();
    }

    @Test
    void loopbackCheckAcceptsALoopbackUriWithoutARawPath() {
        URI origin = origin("localhost", null);

        assertThat(properties(List.of(origin)).localCorsOrigins()).containsExactly(origin);
    }

    @Test
    void loopbackCheckRejectsMalformedBracketedHost() {
        assertThatThrownBy(() -> properties(List.of(origin("[::1", ""))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loopback HTTP origins");
    }

    @ParameterizedTest
    @MethodSource("invalidIpv4LoopbackHosts")
    void rejectsMalformedIpv4LoopbackHost(String host) {
        assertThatThrownBy(() -> properties(List.of(origin(host, ""))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loopback HTTP origins");
    }

    @ParameterizedTest
    @MethodSource("invalidRateLimitDurations")
    void rejectsNonCanonicalRateLimitDurations(
            Duration idleTtl, Duration globalWindow, Duration clientRefill) {
        assertThatThrownBy(() -> new WebProperties.RateLimit(
                        true, 10_000, idleTtl, 60, globalWindow, 5, clientRefill))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("canonical values");
    }

    @ParameterizedTest
    @MethodSource("nullRateLimitDurations")
    void requiresEveryRateLimitDuration(
            Duration idleTtl, Duration globalWindow, Duration clientRefill, String field) {
        assertThatThrownBy(() -> new WebProperties.RateLimit(
                        true, 10_000, idleTtl, 60, globalWindow, 5, clientRefill))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(field);
    }

    private static Stream<String> invalidLocalOrigins() {
        return Stream.of(
                "https://127.0.0.1:3000",
                "HTTP://localhost:3000",
                "http:localhost",
                "http://example.invalid:3000",
                "http://126.0.0.1:3000",
                "http://localhost:3000/path",
                "http://user@localhost:3000",
                "http://localhost:3000?query=value",
                "http://localhost:3000#fragment",
                "http://localhost:0");
    }

    private static Stream<String> validLocalOrigins() {
        return Stream.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://127.0.0.42:3000",
                "http://[::1]:3000");
    }

    private static WebProperties properties(List<URI> origins) {
        return new WebProperties(16_384, CANONICAL_RATE_LIMIT, origins);
    }

    private static Stream<String> invalidIpv4LoopbackHosts() {
        return Stream.of("126.0.0.1", "127.-1.0.1", "127.256.0.1", "127.x.0.1");
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidRateLimitDurations() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        Duration.ofMinutes(59), Duration.ofMinutes(1), Duration.ofMinutes(1)),
                org.junit.jupiter.params.provider.Arguments.of(
                        Duration.ofHours(1), Duration.ofSeconds(59), Duration.ofMinutes(1)),
                org.junit.jupiter.params.provider.Arguments.of(
                        Duration.ofHours(1), Duration.ofMinutes(1), Duration.ofSeconds(59)));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> nullRateLimitDurations() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        null, Duration.ofMinutes(1), Duration.ofMinutes(1), "clientIdleTtl"),
                org.junit.jupiter.params.provider.Arguments.of(
                        Duration.ofHours(1), null, Duration.ofMinutes(1), "globalWindow"),
                org.junit.jupiter.params.provider.Arguments.of(
                        Duration.ofHours(1), Duration.ofMinutes(1), null, "clientRefill"));
    }

    private static URI origin(String host, String rawPath) {
        URI origin = mock(URI.class);
        when(origin.getScheme()).thenReturn("http");
        when(origin.getHost()).thenReturn(host);
        when(origin.getRawPath()).thenReturn(rawPath);
        when(origin.getPort()).thenReturn(3000);
        return origin;
    }

    private static boolean invokeLoopbackCheck(URI origin) {
        try {
            Method method = WebProperties.class.getDeclaredMethod("isLoopbackHttpOrigin", URI.class);
            method.setAccessible(true);
            return (boolean) method.invoke(null, origin);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new AssertionError(exception);
        } catch (InvocationTargetException exception) {
            throw new AssertionError(exception.getCause());
        }
    }
}
