package ru.andrew.website.leads;

import java.security.MessageDigest;
import java.util.Objects;

public final class LeadFingerprint {
    private static final int SHA_256_BYTES = 32;

    private final byte[] bytes;

    public LeadFingerprint(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length != SHA_256_BYTES) {
            throw new IllegalArgumentException("HMAC-SHA-256 must be 32 bytes");
        }
        this.bytes = bytes.clone();
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    public boolean matches(byte[] candidate) {
        return candidate != null && MessageDigest.isEqual(bytes, candidate);
    }
}
