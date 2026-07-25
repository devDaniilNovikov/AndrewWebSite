package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TelegramMessageFormatterTest {
    private static final UUID REQUEST_ID =
            UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Instant CREATED_AT =
            Instant.parse("2026-01-01T00:00:00Z");
    private final TelegramMessageFormatter formatter = new TelegramMessageFormatter();

    @Test
    void formatsDeterministicPlainTextInCanonicalOrder() {
        String formatted = formatter.format(message("Иван", "79991234567",
                "Не охлаждает <витрина> & шумит", "/service/", "repair"));

        assertThat(formatted).isEqualTo("""
                ID заявки: 11111111-1111-4111-8111-111111111111
                Время UTC: 2026-01-01T00:00:00Z
                Тип: repair
                Источник: /service/
                Имя: Иван
                Телефон: 79991234567
                Комментарий: Не охлаждает <витрина> & шумит""");
        assertThat(formatted).doesNotContain("<b>", "&lt;", "&amp;");
    }

    @Test
    void omitsCommentLineWhenCommentIsAbsent() {
        String formatted = formatter.format(
                message("Иван", "79991234567", null, "/service/", "maintenance"));

        assertThat(formatted).doesNotContain("Комментарий:");
        assertThat(formatted).endsWith("Телефон: 79991234567");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\r", "\n", "\r\n", "\u000B", "\f", "\u0085", "\u2028", "\u2029"})
    void neutralizesUserControlledLineSeparatorsWithoutCreatingMessageFields(
            String lineSeparator) {
        String formatted = formatter.format(message(
                "Иван" + lineSeparator + "Телефон: подмена",
                "79991234567" + lineSeparator + "Комментарий: подмена",
                "Не охлаждает" + lineSeparator + "ID заявки: подмена",
                "/service/" + lineSeparator + "Имя: подмена",
                "repair" + lineSeparator + "Источник: подмена"));

        assertThat(formatted).isEqualTo("""
                ID заявки: 11111111-1111-4111-8111-111111111111
                Время UTC: 2026-01-01T00:00:00Z
                Тип: repair Источник: подмена
                Источник: /service/ Имя: подмена
                Имя: Иван Телефон: подмена
                Телефон: 79991234567 Комментарий: подмена
                Комментарий: Не охлаждает ID заявки: подмена""");
    }

    @Test
    void maximumAcceptedLeadFitsTelegramTextLimitWithoutMarkupExpansion() {
        TelegramLeadMessage maximum = message(
                "Я".repeat(100),
                "9".repeat(15),
                "Ю".repeat(1_000),
                "/" + "😀".repeat(2_047),
                "maintenance");

        String formatted = formatter.format(maximum);

        assertThat(formatted.codePointCount(0, formatted.length()))
                .isLessThanOrEqualTo(TelegramMessageFormatter.MAX_TEXT_CODE_POINTS);
    }

    @Test
    void rejectsUnexpectedOversizedProjectionInsteadOfTruncatingLeadData() {
        TelegramLeadMessage oversized =
                message("Иван", "79991234567", "x".repeat(5_000), "/service/", "repair");

        assertThatThrownBy(() -> formatter.format(oversized))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Telegram message exceeds supported text length")
                .hasMessageNotContaining("x".repeat(100));
    }

    @Test
    void messageToStringNeverContainsLeadPiiOrRequestId() {
        TelegramLeadMessage message = message(
                "Иван", "79991234567", "Не охлаждает", "/service/", "repair");

        assertThat(message.toString())
                .doesNotContain(
                        "Иван",
                        "79991234567",
                        "Не охлаждает",
                        "/service/",
                        "repair",
                        REQUEST_ID.toString(),
                        CREATED_AT.toString());
    }

    private static TelegramLeadMessage message(
            String name,
            String phone,
            String comment,
            String sourcePath,
            String intent) {
        return new TelegramLeadMessage(
                7L, REQUEST_ID, name, phone, comment, sourcePath, intent, CREATED_AT);
    }
}
