package ru.andrew.website.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.web")
public record WebProperties(
        @DefaultValue("16384") @Min(16_384) @Max(16_384) int maxRequestBytes,
        @DefaultValue @Valid @NotNull RateLimit rateLimit,
        @DefaultValue List<URI> localCorsOrigins) {

    public WebProperties {
        Objects.requireNonNull(rateLimit, "rateLimit");
        localCorsOrigins = localCorsOrigins == null ? List.of() : List.copyOf(localCorsOrigins);
        if (localCorsOrigins.stream().anyMatch(origin -> !isLoopbackHttpOrigin(origin))) {
            throw new IllegalArgumentException(
                    "app.web.local-cors-origins must contain loopback HTTP origins only");
        }
    }

    private static boolean isLoopbackHttpOrigin(URI origin) {
        if (origin == null
                || !"http".equals(origin.getScheme())
                || origin.getHost() == null
                || origin.getRawUserInfo() != null
                || origin.getRawQuery() != null
                || origin.getRawFragment() != null
                || (origin.getRawPath() != null && !origin.getRawPath().isEmpty())
                || origin.getPort() == 0) {
            return false;
        }
        String host = origin.getHost().toLowerCase(Locale.ROOT);
        return "localhost".equals(host) || "::1".equals(stripIpv6Brackets(host)) || isIpv4Loopback(host);
    }

    private static String stripIpv6Brackets(String host) {
        return host.startsWith("[") && host.endsWith("]")
                ? host.substring(1, host.length() - 1)
                : host;
    }

    private static boolean isIpv4Loopback(String host) {
        String[] octets = host.split("\\.", -1);
        if (octets.length != 4 || !"127".equals(octets[0])) {
            return false;
        }
        for (String octet : octets) {
            try {
                int value = Integer.parseInt(octet);
                if (value < 0 || value > 255) {
                    return false;
                }
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        return true;
    }

    public record RateLimit(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("10000") @Min(10_000) @Max(10_000) int maxClients,
            @DefaultValue("1h") @NotNull Duration clientIdleTtl,
            @DefaultValue("60") @Min(60) @Max(60) int globalLimit,
            @DefaultValue("1m") @NotNull Duration globalWindow,
            @DefaultValue("5") @Min(5) @Max(5) int clientCapacity,
            @DefaultValue("1m") @NotNull Duration clientRefill) {

        public RateLimit {
            Objects.requireNonNull(clientIdleTtl, "clientIdleTtl");
            Objects.requireNonNull(globalWindow, "globalWindow");
            Objects.requireNonNull(clientRefill, "clientRefill");
            if (!Duration.ofHours(1).equals(clientIdleTtl)
                    || !Duration.ofMinutes(1).equals(globalWindow)
                    || !Duration.ofMinutes(1).equals(clientRefill)) {
                throw new IllegalArgumentException(
                        "app.web rate-limit durations must use the canonical values");
            }
        }
    }
}
