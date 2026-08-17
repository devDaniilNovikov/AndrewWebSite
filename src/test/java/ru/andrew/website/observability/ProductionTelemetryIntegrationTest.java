package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.registry.otlp.OtlpMetricsSender;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ErrorHandler;
import ru.andrew.website.leads.AcceptanceOutcome;
import ru.andrew.website.leads.LeadAcceptanceTransaction;
import ru.andrew.website.privacy.RetentionHeartbeat;
import ru.andrew.website.privacy.RetentionMetrics;
import ru.andrew.website.telegram.OutboxRepository;
import ru.andrew.website.telegram.TelegramMetrics;
import ru.andrew.website.telegram.WorkerHeartbeat;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
        NO_DATABASE,
        "LEAD_FINGERPRINT_HMAC_KEY="
                + "fictional-production-fingerprint-key-material-0001",
        "TELEGRAM_BOT_TOKEN=fictional-telegram-token",
        "TELEGRAM_CHAT_ID=fictional-telegram-chat",
        "OTLP_METRICS_URL=https://collector.invalid/v1/metrics",
        "OTLP_AUTHORIZATION=Bearer fictional-otlp-authorization",
        "SENTRY_DSN=https://publickey@o1.ingest.sentry.io/1",
        "test.sentry.capture-transport=true",
        "OTEL_RESOURCE_ATTRIBUTES="
                + "private=fictional-resource-marker,"
                + "request.id=fictional-resource-request",
        "OTEL_SERVICE_NAME=fictional-private-service",
        "spring.application.group=fictional-private-group",
        "spring.datasource.url="
                + "jdbc:postgresql://fictional-db-user:@db.invalid/private"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Import(ProductionTelemetryIntegrationTest.CaptureConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductionTelemetryIntegrationTest {
    private static final String REQUEST_ID =
            "77777777-7777-4777-8777-777777777777";
    private static final String PHONE = "+7 999 123-45-67";
    private static final String COMMENT =
            "fictional-private-comment";
    private static final String EXCEPTION_DETAIL =
            "fictional-exception-detail";
    private static final String EXPORT_FAILURE_DETAIL =
            "fictional-export-failure-detail";
    private static final String RESOURCE_SECRET =
            "fictional-resource-secret";

    @Autowired
    MockMvc mvc;

    @Autowired
    OtlpMeterRegistry otlp;

    @Autowired
    CapturingSender sender;

    @Autowired
    ApplicationAvailability availability;

    @Autowired
    @Qualifier("scheduledTaskErrorHandler")
    ErrorHandler scheduledTaskErrorHandler;

    @MockitoBean
    LeadAcceptanceTransaction transaction;

    @Test
    @Order(1)
    void productionUsesEcsAndDropsSensitiveDomainAndConfigurationData(
            CapturedOutput output) throws Exception {
        when(transaction.accept(any(), any()))
                .thenThrow(new DataAccessResourceFailureException(
                        EXCEPTION_DETAIL + " " + REQUEST_ID + " "
                                + PHONE + " " + COMMENT));

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isServiceUnavailable());
        scheduledTaskErrorHandler.handleError(
                new IllegalStateException(
                        EXCEPTION_DETAIL + " " + REQUEST_ID + " "
                                + PHONE + " " + COMMENT));
        publishNow(otlp);

        assertThat(otlp).isNotNull();
        assertThat(sender).isNotNull();
        OtlpMetricsSender.Request exported = sender.last();
        assertThat(exported).isNotNull();
        assertThat(exported.getHeaders())
                .containsExactly(
                        java.util.Map.entry(
                                "Authorization",
                                "Bearer fictional-otlp-authorization"));
        byte[] payload = exported.getMetricsData();
        ExportMetricsServiceRequest request =
                ExportMetricsServiceRequest.parseFrom(payload);
        assertThat(request.getResourceMetricsList())
                .allSatisfy(resource ->
                        OtlpExportPrivacyTest.assertCanonicalResource(
                                resource.getResource()
                                        .getAttributesList()));
        assertThat(new String(
                        payload, StandardCharsets.ISO_8859_1))
                .doesNotContain(
                        RESOURCE_SECRET,
                        "fictional-resource-request",
                        "fictional-private-service",
                        "fictional-private-group");
        assertThat(output.getAll()).doesNotContain(
                "Иван",
                PHONE,
                COMMENT,
                REQUEST_ID,
                EXCEPTION_DETAIL,
                "fictional-production-fingerprint-key-material-0001",
                "fictional-telegram-token",
                "fictional-telegram-chat",
                "fictional-db-user",
                "fictional-db-password",
                "https://collector.invalid/v1/metrics",
                "fictional-otlp-authorization");
        assertThat(output.getAll().lines()
                        .filter(line -> line.startsWith("{"))
                        .map(ProductionTelemetryIntegrationTest::parseJson)
                        .anyMatch(node ->
                                node.path("ecs").has("version")))
                .isTrue();
    }

    @Test
    @Order(2)
    void exporterFailureCannotBreakApplicationWorkOrLeakItsCause(
            CapturedOutput output) throws Exception {
        int attemptsBeforeFailure = sender.attempts();
        sender.fail();

        assertThatCode(() -> publishNow(otlp))
                .doesNotThrowAnyException();
        assertThat(sender.attempts())
                .isGreaterThan(attemptsBeforeFailure);
        assertThat(otlp.isClosed()).isFalse();

        when(transaction.accept(any(), any()))
                .thenReturn(AcceptanceOutcome.CREATED);
        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isAccepted());
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isNotFound());
        assertThat(availability.getLivenessState())
                .isEqualTo(LivenessState.CORRECT);

        Clock clock = Clock.fixed(
                Instant.parse("2026-07-26T00:00:00Z"),
                ZoneOffset.UTC);
        assertThatCode(() -> {
            var telegram = new TelegramMetrics(
                    otlp,
                    mock(OutboxRepository.class),
                    new WorkerHeartbeat(clock),
                    clock);
            telegram.delivery("delivered", "success");
            var retention = new RetentionMetrics(
                    otlp, new RetentionHeartbeat(clock), clock);
            retention.anonymized(1);
            retention.deleted(1);
        }).doesNotThrowAnyException();
        assertThat(output.getAll()).doesNotContain(
                EXPORT_FAILURE_DETAIL,
                "https://collector.invalid/v1/metrics",
                "fictional-otlp-authorization");
    }

    static tools.jackson.databind.JsonNode parseJson(String line) {
        try {
            return JsonMapper.builder().build().readTree(line);
        } catch (tools.jackson.core.JacksonException invalidJson) {
            throw new AssertionError(invalidJson);
        }
    }

    private static void publishNow(OtlpMeterRegistry registry) {
        ReflectionTestUtils.invokeMethod(registry, "publish");
    }

    private static String validBody() {
        return """
                {"requestId":"%s","name":"Иван","phone":"%s",
                 "comment":"%s","sourcePath":"/service/","intent":"repair",
                 "consent":true,"website":""}
                """.formatted(REQUEST_ID, PHONE, COMMENT);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CaptureConfiguration {
        @Bean
        CapturingSender capturingSender() {
            return new CapturingSender();
        }
    }

    static final class CapturingSender implements OtlpMetricsSender {
        private final AtomicReference<Request> last =
                new AtomicReference<>();
        private final AtomicBoolean failing =
                new AtomicBoolean();
        private final AtomicInteger attempts =
                new AtomicInteger();

        @Override
        public void send(Request request) {
            attempts.incrementAndGet();
            last.set(request);
            if (failing.get()) {
                throw new IllegalStateException(
                        EXPORT_FAILURE_DETAIL);
            }
        }

        void fail() {
            failing.set(true);
        }

        int attempts() {
            return attempts.get();
        }

        Request last() {
            return last.get();
        }
    }
}
