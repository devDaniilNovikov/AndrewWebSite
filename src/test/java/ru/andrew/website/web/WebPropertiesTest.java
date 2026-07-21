package ru.andrew.website.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WebPropertiesTest {
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

    private static Stream<String> invalidLocalOrigins() {
        return Stream.of(
                "https://127.0.0.1:3000",
                "http://example.invalid:3000",
                "http://localhost:3000/path",
                "http://user@localhost:3000",
                "http://localhost:3000?query=value");
    }

    private static Stream<String> validLocalOrigins() {
        return Stream.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://127.0.0.42:3000",
                "http://[::1]:3000");
    }

    private static WebProperties properties(List<URI> origins) {
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
                origins);
    }
}
