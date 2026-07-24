package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LeadFingerprintServiceTest {
    private static final String TEST_KEY =
            "test-only-key-material-not-for-production-0001";

    private final LeadFingerprintService fingerprints =
            new LeadFingerprintService(new LeadProperties(TEST_KEY));

    @Test
    void emitsThePinnedCanonicalDigest() {
        LeadFingerprint fingerprint = fingerprints.fingerprint(lead(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                Instant.parse("2026-07-24T12:00:00Z"),
                null));

        assertThat(HexFormat.of().formatHex(fingerprint.bytes()))
                .isEqualTo("8d4008eb2a7dadeee5db507f20e4dbbb547cef22f63668ed894aa0dfe0c54516");
    }

    @Test
    void canonicalJsonEscapingAndFieldOrderRemainPinned() {
        NormalizedLead lead = new NormalizedLead(
                UUID.fromString("17171717-1717-4717-8717-171717171717"),
                "A\"\\B",
                "79991234567",
                "line\n\"quoted\"\\slash",
                "/a b/",
                LeadIntent.maintenance,
                Instant.parse("2026-07-24T12:00:00Z"));

        assertThat(HexFormat.of().formatHex(fingerprints.fingerprint(lead).bytes()))
                .isEqualTo("b914f8c1a2d05754561ddb52ae4d96071599b6b783a0ae6b038bfce8b4ee9d08");
    }

    @Test
    void excludesRequestIdAndConsentTimeButIncludesPayloadChanges() {
        LeadFingerprint first = fingerprints.fingerprint(lead(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                Instant.parse("2026-07-24T12:00:00Z"),
                "Комментарий"));
        LeadFingerprint equivalent = fingerprints.fingerprint(lead(
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                Instant.parse("2026-07-25T12:00:00Z"),
                "Комментарий"));
        LeadFingerprint changed = fingerprints.fingerprint(lead(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                Instant.parse("2026-07-24T12:00:00Z"),
                "Другой комментарий"));

        assertThat(first.matches(equivalent.bytes())).isTrue();
        assertThat(first.matches(changed.bytes())).isFalse();
    }

    @Test
    void fingerprintDefensivelyCopiesInputAndOutput() {
        byte[] source = new byte[32];
        LeadFingerprint fingerprint = new LeadFingerprint(source);
        source[0] = 1;
        byte[] returned = fingerprint.bytes();
        returned[1] = 2;

        assertThat(fingerprint.bytes()).containsOnly(0);
        assertThat(fingerprint.matches(null)).isFalse();
    }

    private static NormalizedLead lead(UUID requestId, Instant consentedAt, String comment) {
        return new NormalizedLead(
                requestId,
                "Иван",
                "79991234567",
                comment,
                "/service/",
                LeadIntent.repair,
                consentedAt);
    }
}
