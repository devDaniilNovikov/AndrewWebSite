package ru.andrew.website.telegram;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.telegram")
public record TelegramClientProperties(
        @NotBlank String botToken,
        @NotBlank String chatId,
        @NotNull URI baseUrl) {

    public TelegramClientProperties {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalArgumentException(
                    "app.telegram.bot-token must not be blank");
        }
        if (chatId == null || chatId.isBlank()) {
            throw new IllegalArgumentException(
                    "app.telegram.chat-id must not be blank");
        }
        if (baseUrl == null || !baseUrl.isAbsolute()) {
            throw new IllegalArgumentException(
                    "app.telegram.base-url must be absolute");
        }
    }

    @Override
    public String toString() {
        return "TelegramClientProperties[botToken=<redacted>, chatId=<redacted>, baseUrl="
                + safeOrigin(baseUrl)
                + "]";
    }

    private static String safeOrigin(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) {
            return "<invalid>";
        }
        String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
        return scheme + "://" + host + port;
    }
}
