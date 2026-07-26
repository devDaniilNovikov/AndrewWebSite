package ru.andrew.website.telegram;

import java.io.IOException;
import java.io.InputStream;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class TelegramRetryAfterParser {
    private static final int MAX_BODY_BYTES = 4_096;

    private final JsonMapper jsonMapper;

    public TelegramRetryAfterParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    OptionalLong parseSeconds(InputStream body) {
        if (body == null) {
            return OptionalLong.empty();
        }
        try {
            byte[] bytes = body.readNBytes(MAX_BODY_BYTES + 1);
            if (bytes.length > MAX_BODY_BYTES) {
                return OptionalLong.empty();
            }
            JsonNode root = jsonMapper.readTree(bytes);
            if (root == null) {
                return OptionalLong.empty();
            }
            JsonNode retryAfter = root.path("parameters").path("retry_after");
            if (!retryAfter.isIntegralNumber() || !retryAfter.canConvertToLong()) {
                return OptionalLong.empty();
            }
            long seconds = retryAfter.longValue();
            return seconds > 0 ? OptionalLong.of(seconds) : OptionalLong.empty();
        } catch (IOException | RuntimeException ignored) {
            return OptionalLong.empty();
        }
    }
}
