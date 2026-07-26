package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class TelegramLeadMessageValidationTest {
    private static final UUID FICTIONAL_REQUEST_ID =
            UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Instant FICTIONAL_CREATED_AT =
            Instant.parse("2026-01-01T00:00:00Z");
    private static final String FICTIONAL_NAME = "Fictional Test User";
    private static final String FICTIONAL_PHONE = "70000000000";
    private static final String FICTIONAL_SOURCE = "/fictional-test/";
    private static final String FICTIONAL_INTENT = "repair";

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveLeadId(long leadId) {
        assertThatThrownBy(() -> message(
                        leadId,
                        FICTIONAL_REQUEST_ID,
                        FICTIONAL_NAME,
                        FICTIONAL_PHONE,
                        "fictional-comment",
                        FICTIONAL_SOURCE,
                        FICTIONAL_INTENT,
                        FICTIONAL_CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("leadId must be positive");
    }

    @Test
    void rejectsNullRequestId() {
        assertThatThrownBy(() -> message(
                        7L,
                        null,
                        FICTIONAL_NAME,
                        FICTIONAL_PHONE,
                        "fictional-comment",
                        FICTIONAL_SOURCE,
                        FICTIONAL_INTENT,
                        FICTIONAL_CREATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("requestId must not be null");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequiredText")
    void rejectsNullOrBlankRequiredText(
            String field,
            String name,
            String phone,
            String sourcePath,
            String intent) {
        assertThatThrownBy(() -> message(
                        7L,
                        FICTIONAL_REQUEST_ID,
                        name,
                        phone,
                        "fictional-comment",
                        sourcePath,
                        intent,
                        FICTIONAL_CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(field + " must not be blank");
    }

    static Stream<Arguments> invalidRequiredText() {
        return Stream.of(
                Arguments.of(
                        "name", null, FICTIONAL_PHONE, FICTIONAL_SOURCE, FICTIONAL_INTENT),
                Arguments.of(
                        "name", " ", FICTIONAL_PHONE, FICTIONAL_SOURCE, FICTIONAL_INTENT),
                Arguments.of(
                        "phone", FICTIONAL_NAME, null, FICTIONAL_SOURCE, FICTIONAL_INTENT),
                Arguments.of(
                        "phone", FICTIONAL_NAME, "\t", FICTIONAL_SOURCE, FICTIONAL_INTENT),
                Arguments.of(
                        "sourcePath", FICTIONAL_NAME, FICTIONAL_PHONE, null, FICTIONAL_INTENT),
                Arguments.of(
                        "sourcePath", FICTIONAL_NAME, FICTIONAL_PHONE, " ", FICTIONAL_INTENT),
                Arguments.of(
                        "intent", FICTIONAL_NAME, FICTIONAL_PHONE, FICTIONAL_SOURCE, null),
                Arguments.of(
                        "intent", FICTIONAL_NAME, FICTIONAL_PHONE, FICTIONAL_SOURCE, "\n"));
    }

    @Test
    void rejectsNullCreationTime() {
        assertThatThrownBy(() -> message(
                        7L,
                        FICTIONAL_REQUEST_ID,
                        FICTIONAL_NAME,
                        FICTIONAL_PHONE,
                        "fictional-comment",
                        FICTIONAL_SOURCE,
                        FICTIONAL_INTENT,
                        null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("createdAt must not be null");
    }

    @Test
    void acceptsAbsentOrBlankOptionalComment() {
        assertThat(message(
                                7L,
                                FICTIONAL_REQUEST_ID,
                                FICTIONAL_NAME,
                                FICTIONAL_PHONE,
                                null,
                                FICTIONAL_SOURCE,
                                FICTIONAL_INTENT,
                                FICTIONAL_CREATED_AT)
                        .comment())
                .isNull();
        assertThat(message(
                                7L,
                                FICTIONAL_REQUEST_ID,
                                FICTIONAL_NAME,
                                FICTIONAL_PHONE,
                                " ",
                                FICTIONAL_SOURCE,
                                FICTIONAL_INTENT,
                                FICTIONAL_CREATED_AT)
                        .comment())
                .isBlank();
    }

    private static TelegramLeadMessage message(
            long leadId,
            UUID requestId,
            String name,
            String phone,
            String comment,
            String sourcePath,
            String intent,
            Instant createdAt) {
        return new TelegramLeadMessage(
                leadId,
                requestId,
                name,
                phone,
                comment,
                sourcePath,
                intent,
                createdAt);
    }
}
