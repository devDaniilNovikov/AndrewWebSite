package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TelegramDeliveryResultTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void retryableResultRejectsMissingCode(String code) {
        assertThatThrownBy(() ->
                        new TelegramDeliveryResult.Retryable(code, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("result code must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void permanentFailureRejectsMissingCode(String code) {
        assertThatThrownBy(() ->
                        new TelegramDeliveryResult.PermanentFailure(code))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("result code must not be blank");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void retryableResultRejectsNonPositiveRetryDelay(long seconds) {
        assertThatThrownBy(() -> new TelegramDeliveryResult.Retryable(
                        "fictional_retry", Duration.ofSeconds(seconds)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retryAfter must be positive");
    }

    @Test
    void retryableResultAcceptsPositiveOrUnspecifiedRetryDelay() {
        assertThat(new TelegramDeliveryResult.Retryable(
                        "fictional_retry", Duration.ofSeconds(1)))
                .extracting(TelegramDeliveryResult.Retryable::retryAfter)
                .isEqualTo(Duration.ofSeconds(1));
        assertThat(new TelegramDeliveryResult.Retryable(
                        "fictional_retry", null))
                .extracting(TelegramDeliveryResult.Retryable::retryAfter)
                .isNull();
    }
}
