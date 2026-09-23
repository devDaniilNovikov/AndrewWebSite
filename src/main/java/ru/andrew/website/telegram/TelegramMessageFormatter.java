package ru.andrew.website.telegram;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public final class TelegramMessageFormatter {
    static final int MAX_TEXT_CODE_POINTS = 4_096;
    private static final Pattern LINE_BREAK_SEQUENCE = Pattern.compile("\\R");
    private static final String OVERSIZED_MESSAGE =
            "Telegram message exceeds supported text length";

    public String format(TelegramLeadMessage message) {
        // The owner reads only the contact details; the other lead fields stay out of the chat.
        String formatted = """
                Имя: %s
                Телефон: %s"""
                .formatted(
                        singleLine(message.name()),
                        singleLine(message.phone()));
        if (message.comment() != null) {
            formatted += "\nКомментарий: " + singleLine(message.comment());
        }
        if (formatted.codePointCount(0, formatted.length()) > MAX_TEXT_CODE_POINTS) {
            throw new IllegalArgumentException(OVERSIZED_MESSAGE);
        }
        return formatted;
    }

    private static String singleLine(String value) {
        return LINE_BREAK_SEQUENCE.matcher(value).replaceAll(" ");
    }
}
