package ru.andrew.website.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Period;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RetentionPropertiesTest {
    private static final Duration ANONYMIZE_AFTER = Duration.ofDays(29);
    private static final Duration HARD_LIMIT = Duration.ofDays(30);
    private static final Period DELETE_AFTER = Period.ofMonths(12);
    private static final Duration POLL_INTERVAL = Duration.ofHours(1);

    @Test
    void acceptsTheCanonicalPrivacyContract() {
        var properties = properties(
                ANONYMIZE_AFTER,
                HARD_LIMIT,
                DELETE_AFTER,
                POLL_INTERVAL,
                100);

        assertThat(properties.batchSize()).isEqualTo(100);
    }

    @ParameterizedTest
    @MethodSource("invalidContracts")
    void rejectsConfigurationThatDriftsFromThePrivacyContract(
            Duration anonymizeAfter,
            Duration hardLimit,
            Period deleteAfter,
            Duration pollInterval,
            int batchSize) {
        assertThatThrownBy(() -> properties(
                        anonymizeAfter,
                        hardLimit,
                        deleteAfter,
                        pollInterval,
                        batchSize))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retention settings violate the privacy contract");
    }

    static Stream<Arguments> invalidContracts() {
        return Stream.of(
                Arguments.of(
                        Duration.ofDays(28),
                        HARD_LIMIT,
                        DELETE_AFTER,
                        POLL_INTERVAL,
                        100),
                Arguments.of(
                        ANONYMIZE_AFTER,
                        Duration.ofDays(31),
                        DELETE_AFTER,
                        POLL_INTERVAL,
                        100),
                Arguments.of(
                        ANONYMIZE_AFTER,
                        HARD_LIMIT,
                        Period.ofMonths(11),
                        POLL_INTERVAL,
                        100),
                Arguments.of(
                        ANONYMIZE_AFTER,
                        HARD_LIMIT,
                        DELETE_AFTER,
                        Duration.ofMinutes(30),
                        100),
                Arguments.of(
                        ANONYMIZE_AFTER,
                        HARD_LIMIT,
                        DELETE_AFTER,
                        POLL_INTERVAL,
                        0),
                Arguments.of(
                        ANONYMIZE_AFTER,
                        HARD_LIMIT,
                        DELETE_AFTER,
                        POLL_INTERVAL,
                        1_001));
    }

    private static RetentionProperties properties(
            Duration anonymizeAfter,
            Duration hardLimit,
            Period deleteAfter,
            Duration pollInterval,
            int batchSize) {
        return new RetentionProperties(
                anonymizeAfter,
                hardLimit,
                deleteAfter,
                pollInterval,
                batchSize);
    }
}
