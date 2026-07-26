package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.env.MockEnvironment;

class TelegramConfigurationTest {
    private static final String BOT_PATH_VALUE =
            "test-only-bot-token-not-a-secret";
    private static final String CHAT = "test-only-chat-not-a-destination";

    @Test
    void propertiesRequireNonBlankCredentialsAndAbsoluteBaseUrl() {
        assertThatThrownBy(() ->
                        new TelegramClientProperties(
                                " ",
                                CHAT,
                                URI.create("https://telegram.invalid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.telegram.bot-token must not be blank");
        assertThatThrownBy(() ->
                        new TelegramClientProperties(
                                BOT_PATH_VALUE,
                                " ",
                                URI.create("https://telegram.invalid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.telegram.chat-id must not be blank");
        assertThatThrownBy(() ->
                        new TelegramClientProperties(BOT_PATH_VALUE, CHAT, URI.create("/relative")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.telegram.base-url must be absolute");
    }

    @Test
    void propertiesToStringRedactsTokenAndDestination() {
        TelegramClientProperties properties = properties("https://api.telegram.org");

        assertThat(properties.toString())
                .doesNotContain(BOT_PATH_VALUE, CHAT)
                .contains("<redacted>", "https://api.telegram.org");
    }

    @Test
    void productionAcceptsOnlyCanonicalTelegramApiOrigin() {
        assertThatCode(() -> guard("prod", "https://api.telegram.org").afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsafeProductionOrigins")
    void productionRejectsEveryNonCanonicalOrigin(String description, String origin) {
        TelegramEndpointGuard guard = guard("prod", origin);

        assertThatThrownBy(guard::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(TelegramEndpointGuard.PRODUCTION_ENDPOINT_MESSAGE)
                .hasMessageNotContaining(BOT_PATH_VALUE)
                .hasMessageNotContaining(CHAT)
                .hasMessageNotContaining(origin);
    }

    static Stream<Arguments> unsafeProductionOrigins() {
        return Stream.of(
                Arguments.of("HTTP", "http://api.telegram.org"),
                Arguments.of("different host", "https://telegram.invalid"),
                Arguments.of("user info", "https://user@api.telegram.org"),
                Arguments.of("explicit port", "https://api.telegram.org:443"),
                Arguments.of("path", "https://api.telegram.org/api"),
                Arguments.of("query", "https://api.telegram.org?debug=true"),
                Arguments.of("fragment", "https://api.telegram.org#debug"));
    }

    @ParameterizedTest
    @MethodSource("safeLocalOrigins")
    void localAcceptsExplicitLoopbackFakeOrigins(String origin) {
        assertThatCode(() -> guard("local", origin).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    static Stream<String> safeLocalOrigins() {
        return Stream.of(
                "http://localhost:18081",
                "http://127.0.0.1:18081",
                "http://[::1]:18081");
    }

    @ParameterizedTest
    @MethodSource("unsafeLocalOrigins")
    void localRejectsRemoteOrAmbiguousFakeOrigins(String origin) {
        TelegramEndpointGuard guard = guard("local", origin);

        assertThatThrownBy(guard::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(TelegramEndpointGuard.LOCAL_ENDPOINT_MESSAGE)
                .hasMessageNotContaining(BOT_PATH_VALUE)
                .hasMessageNotContaining(CHAT)
                .hasMessageNotContaining(origin);
    }

    static Stream<String> unsafeLocalOrigins() {
        return Stream.of(
                "https://api.telegram.org",
                "http://localhost",
                "ftp://localhost:18081",
                "http://user@localhost:18081",
                "http://localhost:18081/path",
                "http://localhost:18081?debug=true",
                "http://localhost:18081#debug");
    }

    @Test
    void testProfileMayUseNonRoutableOrLoopbackFakeOrigin() {
        assertThatCode(() -> guard("test", "https://telegram.invalid").afterPropertiesSet())
                .doesNotThrowAnyException();
        assertThatCode(() -> guard("test", "http://127.0.0.1:18081").afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("unsafeTestOrigins")
    void testProfileRejectsEveryRoutableOrigin(String origin) {
        TelegramEndpointGuard guard = guard("test", origin);

        assertThatThrownBy(guard::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Test Telegram endpoint must be non-routable or loopback")
                .hasMessageNotContaining(BOT_PATH_VALUE)
                .hasMessageNotContaining(CHAT)
                .hasMessageNotContaining(origin);
    }

    static Stream<String> unsafeTestOrigins() {
        return Stream.of(
                "https://api.telegram.org",
                "https://example.com",
                "https://sub.telegram.invalid");
    }

    private static TelegramEndpointGuard guard(String profile, String origin) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return new TelegramEndpointGuard(properties(origin), environment);
    }

    private static TelegramClientProperties properties(String origin) {
        return new TelegramClientProperties(
                BOT_PATH_VALUE, CHAT, URI.create(origin));
    }
}
