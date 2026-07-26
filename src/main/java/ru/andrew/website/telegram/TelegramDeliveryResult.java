package ru.andrew.website.telegram;

import java.time.Duration;

public sealed interface TelegramDeliveryResult
        permits TelegramDeliveryResult.Delivered,
                TelegramDeliveryResult.Retryable,
                TelegramDeliveryResult.PermanentFailure {

    record Delivered() implements TelegramDeliveryResult {
    }

    record Retryable(String code, Duration retryAfter)
            implements TelegramDeliveryResult {
        public Retryable {
            requireCode(code);
            if (retryAfter != null && (retryAfter.isZero() || retryAfter.isNegative())) {
                throw new IllegalArgumentException("retryAfter must be positive");
            }
        }
    }

    record PermanentFailure(String code) implements TelegramDeliveryResult {
        public PermanentFailure {
            requireCode(code);
        }
    }

    private static void requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("result code must not be blank");
        }
    }
}
