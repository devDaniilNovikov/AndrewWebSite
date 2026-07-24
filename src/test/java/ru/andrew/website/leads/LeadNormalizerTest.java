package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class LeadNormalizerTest {
    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    private final LeadNormalizer normalizer = new LeadNormalizer();

    @Test
    void normalizesUnicodeWhitespacePhoneAndBlankCommentWithoutMutatingInput() {
        LeadRequest request = request(
                "  A\u030A Service  ",
                "+7 (999) 123-45-67",
                " \t ",
                " /service/ ");

        NormalizedLead normalized = normalizer.normalize(request, NOW);

        assertThat(normalized.name()).isEqualTo("Å Service");
        assertThat(normalized.phoneDigits()).isEqualTo("79991234567");
        assertThat(normalized.comment()).isNull();
        assertThat(normalized.sourcePath()).isEqualTo("/service/");
        assertThat(normalized.consentedAt()).isEqualTo(NOW);
        assertThat(request.name()).isEqualTo("  A\u030A Service  ");
    }

    @Test
    void countsNormalizedTextBoundsByUnicodeCodePoint() {
        String acceptedName = "😀".repeat(100);

        String normalizedName = normalizer.normalize(
                        request(acceptedName, "79991234567", null, "/service/"), NOW)
                .name();
        assertThat(normalizedName.codePointCount(0, normalizedName.length())).isEqualTo(100);

        assertThatThrownBy(() -> normalizer.normalize(
                        request(acceptedName + "😀", "79991234567", null, "/service/"), NOW))
                .isInstanceOf(InvalidLeadRequestException.class);
    }

    @Test
    void validatesPhoneInputLengthAfterTrimByUnicodeCodePoint() {
        String trailingWhitespace = "1234567" + " ".repeat(30);
        assertThat(normalizer.normalize(
                                request("Иван", trailingWhitespace, null, "/service/"), NOW)
                        .phoneDigits())
                .isEqualTo("1234567");

        String overLimit = "1 2 3 4 5 6 7" + " ".repeat(19) + "8";
        assertThatThrownBy(() -> normalizer.normalize(
                        request("Иван", overLimit, null, "/service/"), NOW))
                .isInstanceOf(InvalidLeadRequestException.class);
    }

    @Test
    void validatesCommentAndSourcePathBoundsByUnicodeCodePoint() {
        NormalizedLead accepted = normalizer.normalize(
                request(
                        "Иван",
                        "79991234567",
                        "😀".repeat(1_000),
                        "/" + "😀".repeat(2_047)),
                NOW);
        assertThat(accepted.comment().codePointCount(0, accepted.comment().length()))
                .isEqualTo(1_000);
        assertThat(accepted.sourcePath().codePointCount(0, accepted.sourcePath().length()))
                .isEqualTo(2_048);

        assertThatThrownBy(() -> normalizer.normalize(
                        request(
                                "Иван",
                                "79991234567",
                                "😀".repeat(1_001),
                                "/service/"),
                        NOW))
                .isInstanceOf(InvalidLeadRequestException.class);
        assertThatThrownBy(() -> normalizer.normalize(
                        request(
                                "Иван",
                                "79991234567",
                                null,
                                "/" + "😀".repeat(2_048)),
                        NOW))
                .isInstanceOf(InvalidLeadRequestException.class);
    }

    @ParameterizedTest
    @MethodSource("invalidSourcePaths")
    void rejectsNonLocalOrUnsafeSourcePaths(String sourcePath) {
        assertThatThrownBy(() -> normalizer.normalize(
                        request("Иван", "79991234567", null, sourcePath), NOW))
                .isInstanceOf(InvalidLeadRequestException.class);
    }

    @ParameterizedTest
    @MethodSource("invalidPhones")
    void rejectsPhonesOutsideNormalizedDigitBounds(String phone) {
        assertThatThrownBy(() -> normalizer.normalize(
                        request("Иван", phone, null, "/service/"), NOW))
                .isInstanceOf(InvalidLeadRequestException.class);
    }

    private static Stream<String> invalidSourcePaths() {
        return Stream.of(
                "",
                "relative/path",
                "//example.invalid/path",
                "/service?from=form",
                "/service#fragment",
                "/service\\path",
                "/service/../admin",
                "/service/\u0001");
    }

    private static Stream<String> invalidPhones() {
        return Stream.of("123456", "1234567890123456");
    }

    private static LeadRequest request(
            String name, String phone, String comment, String sourcePath) {
        return new LeadRequest(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                name,
                phone,
                comment,
                sourcePath,
                LeadIntent.repair,
                true,
                "");
    }
}
