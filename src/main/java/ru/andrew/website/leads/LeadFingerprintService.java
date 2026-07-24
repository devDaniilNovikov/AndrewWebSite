package ru.andrew.website.leads;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
public final class LeadFingerprintService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final JsonMapper CANONICAL_JSON = JsonMapper.builder().build();

    private final byte[] key;

    public LeadFingerprintService(LeadProperties properties) {
        this.key = properties.fingerprintKey().getBytes(StandardCharsets.UTF_8);
    }

    public LeadFingerprint fingerprint(NormalizedLead lead) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return new LeadFingerprint(mac.doFinal(canonicalBytes(lead)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA-256 is unavailable", exception);
        }
    }

    private static byte[] canonicalBytes(NormalizedLead lead) {
        ObjectNode payload = CANONICAL_JSON.createObjectNode();
        payload.put("name", lead.name());
        payload.put("phone", lead.phoneDigits());
        if (lead.comment() == null) {
            payload.putNull("comment");
        } else {
            payload.put("comment", lead.comment());
        }
        payload.put("sourcePath", lead.sourcePath());
        payload.put("intent", lead.intent().name());
        payload.put("consent", true);
        try {
            return CANONICAL_JSON.writeValueAsBytes(payload);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Canonical JSON serialization failed", exception);
        }
    }
}
