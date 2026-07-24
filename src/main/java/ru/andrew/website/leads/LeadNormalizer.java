package ru.andrew.website.leads;

import java.text.Normalizer;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public final class LeadNormalizer {
    private static final int MIN_NAME_CODE_POINTS = 2;
    private static final int MAX_NAME_CODE_POINTS = 100;
    private static final int MAX_COMMENT_CODE_POINTS = 1_000;
    private static final int MAX_SOURCE_PATH_CODE_POINTS = 2_048;
    private static final int MIN_PHONE_INPUT_CODE_POINTS = 7;
    private static final int MAX_PHONE_INPUT_CODE_POINTS = 32;
    private static final int MIN_PHONE_DIGITS = 7;
    private static final int MAX_PHONE_DIGITS = 15;

    public NormalizedLead normalize(LeadRequest request, Instant consentedAt) {
        String name = normalizedRequired(request.name());
        String phone = normalizedRequired(request.phone());
        String phoneDigits = phoneDigits(phone);
        String comment = normalizedOptional(request.comment());
        String sourcePath = normalizedRequired(request.sourcePath());

        requireCodePointRange(name, MIN_NAME_CODE_POINTS, MAX_NAME_CODE_POINTS);
        requireCodePointRange(
                phone, MIN_PHONE_INPUT_CODE_POINTS, MAX_PHONE_INPUT_CODE_POINTS);
        requireCodePointRange(phoneDigits, MIN_PHONE_DIGITS, MAX_PHONE_DIGITS);
        if (comment != null) {
            requireCodePointRange(comment, 0, MAX_COMMENT_CODE_POINTS);
        }
        requireCodePointRange(sourcePath, 1, MAX_SOURCE_PATH_CODE_POINTS);
        requireLocalSourcePath(sourcePath);

        return new NormalizedLead(
                request.requestId(),
                name,
                phoneDigits,
                comment,
                sourcePath,
                request.intent(),
                consentedAt);
    }

    private static String normalizedRequired(String value) {
        if (value == null) {
            throw new InvalidLeadRequestException();
        }
        String normalized = Normalizer.normalize(value.strip(), Normalizer.Form.NFC);
        if (normalized.isEmpty()) {
            throw new InvalidLeadRequestException();
        }
        return normalized;
    }

    private static String normalizedOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value.strip(), Normalizer.Form.NFC);
        return normalized.isEmpty() ? null : normalized;
    }

    private static String phoneDigits(String value) {
        if (value == null) {
            throw new InvalidLeadRequestException();
        }
        StringBuilder digits = new StringBuilder();
        value.codePoints().filter(Character::isDigit).forEach(codePoint -> {
            int digit = Character.digit(codePoint, 10);
            if (digit < 0) {
                throw new InvalidLeadRequestException();
            }
            digits.append((char) ('0' + digit));
        });
        return digits.toString();
    }

    private static void requireCodePointRange(String value, int minimum, int maximum) {
        int codePoints = value.codePointCount(0, value.length());
        if (codePoints < minimum || codePoints > maximum) {
            throw new InvalidLeadRequestException();
        }
    }

    private static void requireLocalSourcePath(String sourcePath) {
        if (!sourcePath.startsWith("/")
                || sourcePath.startsWith("//")
                || sourcePath.indexOf('?') >= 0
                || sourcePath.indexOf('#') >= 0
                || sourcePath.indexOf('\\') >= 0
                || sourcePath.codePoints().anyMatch(codePoint -> codePoint <= 0x1f)) {
            throw new InvalidLeadRequestException();
        }
        for (String segment : sourcePath.split("/", -1)) {
            if ("..".equals(segment)) {
                throw new InvalidLeadRequestException();
            }
        }
    }
}
