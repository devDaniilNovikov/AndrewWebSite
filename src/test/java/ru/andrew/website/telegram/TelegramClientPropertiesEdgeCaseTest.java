package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import org.junit.jupiter.api.Test;

class TelegramClientPropertiesEdgeCaseTest {
    private static final String FICTIONAL_TOKEN =
            "test-only-token-not-a-secret";
    private static final String FICTIONAL_CHAT =
            "test-only-chat-not-a-destination";

    @Test
    void propertiesRejectNullTokenBeforeInspectingOtherValues() {
        assertThatThrownBy(() -> new TelegramClientProperties(
                        null,
                        FICTIONAL_CHAT,
                        URI.create("https://telegram.invalid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.telegram.bot-token must not be blank");
    }

    @Test
    void propertiesRejectNullChatId() {
        assertThatThrownBy(() -> new TelegramClientProperties(
                        FICTIONAL_TOKEN,
                        null,
                        URI.create("https://telegram.invalid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.telegram.chat-id must not be blank");
    }

    @Test
    void propertiesRejectNullBaseUrl() {
        assertThatThrownBy(() -> new TelegramClientProperties(
                        FICTIONAL_TOKEN, FICTIONAL_CHAT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.telegram.base-url must be absolute");
    }

    @Test
    void toStringReportsOpaqueAbsoluteUriAsInvalidWithoutLeakingCredentials() {
        TelegramClientProperties properties = properties(
                "urn:fictional:telegram");

        assertThat(properties.toString())
                .contains("baseUrl=<invalid>")
                .doesNotContain(FICTIONAL_TOKEN, FICTIONAL_CHAT);
    }

    @Test
    void toStringIncludesExplicitPortButOmitsPathQueryAndFragment() {
        TelegramClientProperties properties = properties(
                "http://localhost:18081/private-path?fictional=query#fragment");

        assertThat(properties.toString())
                .contains("baseUrl=http://localhost:18081")
                .doesNotContain(
                        FICTIONAL_TOKEN,
                        FICTIONAL_CHAT,
                        "private-path",
                        "fictional=query",
                        "fragment");
    }

    @Test
    void defensiveOriginFormatterReportsUriWithoutSchemeAsInvalid() {
        assertThat(invokeSafeOrigin(URI.create("//localhost:18081")))
                .isEqualTo("<invalid>");
    }

    private static TelegramClientProperties properties(String origin) {
        return new TelegramClientProperties(
                FICTIONAL_TOKEN, FICTIONAL_CHAT, URI.create(origin));
    }

    private static String invokeSafeOrigin(URI uri) {
        try {
            Method method = TelegramClientProperties.class.getDeclaredMethod(
                    "safeOrigin", URI.class);
            method.setAccessible(true);
            return (String) method.invoke(null, uri);
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException reflectionFailure) {
            throw new AssertionError(reflectionFailure);
        }
    }
}
