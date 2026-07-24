package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class LeadPropertiesTest {
    @Test
    void requiresEnoughUtf8KeyMaterialWithoutExposingIt() {
        assertThatThrownBy(() -> new LeadProperties("x".repeat(31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UTF-8 bytes");

        LeadProperties properties = new LeadProperties("секрет".repeat(8));

        assertThat(properties.toString()).doesNotContain(properties.fingerprintKey());
    }

    @Test
    void canonicalTestKeyIsRejectedOutsideTheTestProfile() {
        var properties =
                new LeadProperties("test-only-key-material-not-for-production-0001");
        var testEnvironment = new MockEnvironment();
        testEnvironment.setActiveProfiles("test");
        var productionEnvironment = new MockEnvironment();
        productionEnvironment.setActiveProfiles("prod");

        assertThatCode(() -> new LeadFingerprintKeyProfileGuard(
                                properties, testEnvironment)
                        .afterPropertiesSet())
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> new LeadFingerprintKeyProfileGuard(
                                properties, productionEnvironment)
                        .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The test fingerprint key is not allowed outside the test profile");
    }
}
