package ru.andrew.website.telegram;

import org.springframework.stereotype.Component;

@Component
public final class TelegramMessageFormatter {
    static final int MAX_TEXT_CODE_POINTS = 4_096;
    private static final String OVERSIZED_MESSAGE =
            "Telegram message exceeds supported text length";

    public String format(TelegramLeadMessage message) {
        String formatted = """
                ID заявки: %s
                Время UTC: %s
                Тип: %s
                Источник: %s
                Имя: %s
                Телефон: %s"""
                .formatted(
                        message.requestId(),
                        message.createdAt(),
                        message.intent(),
                        message.sourcePath(),
                        message.name(),
                        message.phone());
        if (message.comment() != null) {
            formatted += "\nКомментарий: " + message.comment();
        }
        if (formatted.codePointCount(0, formatted.length()) > MAX_TEXT_CODE_POINTS) {
            throw new IllegalArgumentException(OVERSIZED_MESSAGE);
        }
        return formatted;
    }
}
