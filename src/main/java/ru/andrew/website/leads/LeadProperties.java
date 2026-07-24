package ru.andrew.website.leads;

import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.leads")
public record LeadProperties(@NotBlank String fingerprintKey) {
    private static final int MINIMUM_KEY_BYTES = 32;

    public LeadProperties {
        if (fingerprintKey == null
                || fingerprintKey.isBlank()
                || fingerprintKey.getBytes(StandardCharsets.UTF_8).length < MINIMUM_KEY_BYTES) {
            throw new IllegalArgumentException(
                    "app.leads.fingerprint-key must contain at least 32 UTF-8 bytes");
        }
    }

    @Override
    public String toString() {
        return "LeadProperties[fingerprintKey=<redacted>]";
    }
}
