package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

class TelegramRetryAfterParserTest {
    private final TelegramRetryAfterParser parser =
            new TelegramRetryAfterParser(JsonMapper.builder().build());

    @Test
    void nullBodyHasNoRetryDelay() {
        assertThat(parser.parseSeconds(null)).isEmpty();
    }

    @Test
    void emptyBodyHasNoRetryDelay() {
        assertThat(parser.parseSeconds(stream(""))).isEmpty();
    }

    @Test
    void ioFailureHasNoRetryDelay() {
        InputStream failingBody = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("fictional-read-failure");
            }
        };

        assertThat(parser.parseSeconds(failingBody)).isEmpty();
    }

    @Test
    void missingJsonRootHasNoRetryDelay() throws JacksonException {
        JsonMapper nullReturningMapper = mock(JsonMapper.class);
        when(nullReturningMapper.readTree(any(byte[].class))).thenReturn(null);

        assertThat(new TelegramRetryAfterParser(nullReturningMapper)
                        .parseSeconds(stream("{}")))
                .isEmpty();
    }

    private static InputStream stream(String body) {
        return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }
}
