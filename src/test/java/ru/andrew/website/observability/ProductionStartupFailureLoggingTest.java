package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProductionStartupFailureLoggingTest {
    private static final Duration TIMEOUT =
            Duration.ofSeconds(30);
    private static final List<String> PRIVATE_FAILURE_DATA =
            List.of(
                    "jdbc:postgresql",
                    "db.invalid",
                    "fictional-db-user",
                    "fictional-db-password",
                    "fictional-startup-authorization",
                    "publickey",
                    "ingest.sentry.io",
                    "UnknownHostException",
                    "error.message",
                    "stack_trace",
                    "started by");

    @Test
    void failedDatabaseStartupEmitsOneGenericEcsEvent()
            throws Exception {
        runFailureProbe(
                List.of("--spring.profiles.active=prod"),
                List.of(
                        "--spring.datasource.url="
                                + "jdbc:postgresql://db.invalid"
                                + ":5432/private",
                        "--spring.datasource.username="
                                + "fictional-db-user",
                        "--spring.datasource.password="
                                + "fictional-db-password"),
                true);
    }

    @Test
    void failedConfigImportEmitsOneGenericEcsEvent()
            throws Exception {
        runFailureProbe(
                List.of("--spring.profiles.active=prod"),
                List.of(
                        "--spring.config.import="
                                + "https://fictional-config-user"
                                + ":fictional-config-password"
                                + "@127.0.0.1:1/private.yml"),
                false);
    }

    @Test
    void repeatedProductionProfileArgumentFailsClosed()
            throws Exception {
        runFailureProbe(
                List.of(
                        "--spring.profiles.active=test",
                        "--spring.profiles.active=prod"),
                List.of(
                        "--spring.config.import="
                                + "https://fictional-config-user"
                                + ":fictional-config-password"
                                + "@127.0.0.1:1/private.yml"),
                false);
    }

    private static void runFailureProbe(
            List<String> profileArguments,
            List<String> failureArguments,
            boolean contextMustInitialize)
            throws Exception {
        Path outputFile = Files.createTempFile(
                "production-startup-failure-", ".log");
        Path initializedMarker = Files.createTempFile(
                "production-startup-initialized-", ".marker");
        Files.delete(initializedMarker);
        Process process = null;
        try {
            process = process(
                    outputFile,
                    initializedMarker,
                    profileArguments,
                    failureArguments).start();
            boolean finished = process.waitFor(
                    TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }
            assertThat(finished)
                    .as("production failure probe must terminate")
                    .isTrue();
            assertThat(process.exitValue()).isNotZero();
            String output = Files.readString(
                    outputFile, StandardCharsets.UTF_8);
            if (contextMustInitialize) {
                assertThat(Files.readString(
                                initializedMarker,
                                StandardCharsets.UTF_8))
                        .isEqualTo("initialized");
            } else {
                assertThat(Files.exists(initializedMarker))
                        .isFalse();
            }
            assertThat(output.lines()
                            .filter(line -> !line.isBlank())
                            .map(ProductionTelemetryIntegrationTest::parseJson)
                            .toList())
                    .singleElement()
                    .satisfies(event -> {
                        assertThat(event.path("message").asText())
                                .isEqualTo(
                                        "Application startup failed");
                        assertThat(event.path("log")
                                        .path("logger").asText())
                                .isEqualTo(
                                        "ru.andrew.website.common"
                                                + ".ProductionStartupFailureReporter");
                        assertThat(event.path("ecs").has("version"))
                                .isTrue();
                        assertThat(event.has("error")).isFalse();
                    });
            boolean privateDataPresent = PRIVATE_FAILURE_DATA.stream()
                    .anyMatch(output::contains);
            assertThat(privateDataPresent)
                    .as(
                            "startup failure output must be detail-free;"
                                    + " markerBitmap=%s",
                            PRIVATE_FAILURE_DATA.stream()
                                    .map(value -> output.contains(value)
                                            ? "1" : "0")
                                    .reduce("", String::concat))
                    .isFalse();
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            Files.deleteIfExists(outputFile);
            Files.deleteIfExists(initializedMarker);
        }
    }

    private static ProcessBuilder process(
            Path outputFile,
            Path initializedMarker,
            List<String> profileArguments,
            List<String> failureArguments) {
        String java = Path.of(
                        System.getProperty("java.home"),
                        "bin",
                        "java")
                .toString();
        String classpath = System.getProperty(
                "surefire.test.class.path",
                System.getProperty("java.class.path"));
        List<String> command = Stream.concat(
                        Stream.of(
                                java,
                                "--enable-native-access=ALL-UNNAMED",
                                "-cp",
                                classpath,
                                "ru.andrew.website"
                                        + ".ProductionStartupFailureProbe",
                                initializedMarker.toString()),
                        Stream.concat(
                                profileArguments.stream(),
                                failureArguments.stream()))
                .toList();
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.redirectOutput(outputFile.toFile());
        Map<String, String> environment = builder.environment();
        environment.clear();
        environment.putAll(Map.of(
                "LEAD_FINGERPRINT_HMAC_KEY",
                "fictional-production-fingerprint-key-material-0001",
                "TELEGRAM_BOT_TOKEN",
                "fictional-telegram-token",
                "TELEGRAM_CHAT_ID",
                "fictional-telegram-chat",
                "OTLP_METRICS_URL",
                "https://collector.invalid/v1/metrics",
                "OTLP_AUTHORIZATION",
                "Bearer fictional-startup-authorization",
                "SENTRY_DSN",
                "https://publickey@o1.ingest.sentry.io/1",
                "TEST_SENTRY_CAPTURE_TRANSPORT",
                "true"));
        return builder;
    }
}
