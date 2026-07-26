package ru.andrew.website.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.andrew.website.leads.LeadAcceptanceTransaction;

@SpringBootTest(properties = {
        "spring.http.clients.connect-timeout=100ms",
        "spring.http.clients.read-timeout=100ms",
        NO_DATABASE
})
@ActiveProfiles("test")
class TelegramBootRestClientIntegrationTest {
    private static final AtomicReference<ResponseMode> RESPONSE_MODE =
            new AtomicReference<>(ResponseMode.REDIRECT);
    private static final AtomicInteger REDIRECT_TARGET_CALLS =
            new AtomicInteger();
    private static HttpServer fakeTelegram;
    private static ExecutorService fakeTelegramExecutor;

    @MockitoBean
    LeadAcceptanceTransaction transaction;

    @Autowired
    TelegramGateway gateway;

    @DynamicPropertySource
    static void fakeTelegramEndpoint(DynamicPropertyRegistry registry)
            throws IOException {
        startFakeTelegram();
        registry.add("app.telegram.base-url", TelegramBootRestClientIntegrationTest::baseUrl);
    }

    @AfterAll
    static void stopFakeTelegram() {
        if (fakeTelegram != null) {
            fakeTelegram.stop(0);
        }
        if (fakeTelegramExecutor != null) {
            fakeTelegramExecutor.close();
        }
    }

    @Test
    void bootManagedClientDoesNotFollowRedirects() {
        RESPONSE_MODE.set(ResponseMode.REDIRECT);
        REDIRECT_TARGET_CALLS.set(0);

        assertThat(gateway.send(message()))
                .isEqualTo(new TelegramDeliveryResult.Retryable(
                        "telegram_unexpected", null));
        assertThat(REDIRECT_TARGET_CALLS).hasValue(0);
    }

    @Test
    void bootManagedClientAppliesReadTimeout() {
        RESPONSE_MODE.set(ResponseMode.SLOW);

        TelegramDeliveryResult result = assertTimeout(
                Duration.ofMillis(750),
                () -> gateway.send(message()));

        assertThat(result)
                .isEqualTo(new TelegramDeliveryResult.Retryable("network", null));
    }

    private static synchronized void startFakeTelegram() throws IOException {
        if (fakeTelegram != null) {
            return;
        }
        fakeTelegram = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        fakeTelegramExecutor = Executors.newVirtualThreadPerTaskExecutor();
        fakeTelegram.setExecutor(fakeTelegramExecutor);
        fakeTelegram.createContext("/", TelegramBootRestClientIntegrationTest::respond);
        fakeTelegram.start();
    }

    private static void respond(HttpExchange exchange) throws IOException {
        try (exchange) {
            if ("/redirect-target".equals(exchange.getRequestURI().getPath())) {
                REDIRECT_TARGET_CALLS.incrementAndGet();
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            if (RESPONSE_MODE.get() == ResponseMode.REDIRECT) {
                exchange.getResponseHeaders()
                        .add("Location", baseUrl() + "/redirect-target");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
            try {
                Thread.sleep(Duration.ofSeconds(1));
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (IOException clientDisconnected) {
                // The expected read timeout closes the client connection.
            }
        }
    }

    private static String baseUrl() {
        return "http://127.0.0.1:" + fakeTelegram.getAddress().getPort();
    }

    private static TelegramLeadMessage message() {
        return new TelegramLeadMessage(
                7L,
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                "Иван",
                "79991234567",
                "Не охлаждает",
                "/service/",
                "repair",
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private enum ResponseMode {
        REDIRECT,
        SLOW
    }
}
