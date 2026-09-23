package ru.andrew.website.telegram;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public final class TelegramEndpointGuard implements InitializingBean {
    static final String PRODUCTION_ENDPOINT_MESSAGE =
            "Production Telegram endpoint must be a bare HTTPS origin";
    static final String LOCAL_ENDPOINT_MESSAGE =
            "Local Telegram endpoint must use an explicit loopback port";
    static final String TEST_ENDPOINT_MESSAGE =
            "Test Telegram endpoint must be non-routable or loopback";

    private static final URI TEST_ORIGIN =
            URI.create("https://telegram.invalid");
    private static final Set<String> LOOPBACK_HOSTS =
            Set.of("localhost", "127.0.0.1", "::1", "[::1]");

    private final TelegramClientProperties properties;
    private final Environment environment;

    public TelegramEndpointGuard(
            TelegramClientProperties properties,
            Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        URI baseUrl = properties.baseUrl();
        if (environment.matchesProfiles("prod") && !isSafeProductionEndpoint(baseUrl)) {
            throw new IllegalStateException(PRODUCTION_ENDPOINT_MESSAGE);
        }
        if (environment.matchesProfiles("local") && !isSafeLocalEndpoint(baseUrl)) {
            throw new IllegalStateException(LOCAL_ENDPOINT_MESSAGE);
        }
        if (environment.matchesProfiles("test")
                && !TEST_ORIGIN.equals(baseUrl)
                && !isSafeLocalEndpoint(baseUrl)) {
            throw new IllegalStateException(TEST_ENDPOINT_MESSAGE);
        }
    }

    // Production uses https://api.telegram.org unless the host network blocks it; then the operator
    // points TELEGRAM_BASE_URL at their own relay. The bot token travels in the request path, so
    // only an encrypted origin without port, path, query, fragment or credentials is accepted.
    private static boolean isSafeProductionEndpoint(URI uri) {
        return "https".equals(uri.getScheme())
                && uri.getHost() != null
                && uri.getPort() == -1
                && uri.getRawUserInfo() == null
                && isEmpty(uri.getRawPath())
                && uri.getRawQuery() == null
                && uri.getRawFragment() == null;
    }

    private static boolean isSafeLocalEndpoint(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        return scheme != null
                && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && host != null
                && LOOPBACK_HOSTS.contains(host.toLowerCase(Locale.ROOT))
                && uri.getPort() > 0
                && uri.getPort() <= 65_535
                && uri.getRawUserInfo() == null
                && isEmpty(uri.getRawPath())
                && uri.getRawQuery() == null
                && uri.getRawFragment() == null;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
