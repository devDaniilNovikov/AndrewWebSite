package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.registry.otlp.OtlpMetricsSender;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.metrics.v1.Metric;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsProperties;
import org.springframework.mock.env.MockEnvironment;
import ru.andrew.website.leads.AcceptanceOutcome;
import ru.andrew.website.leads.LeadMetrics;
import ru.andrew.website.leads.LeadRejectionReason;

class OtlpExportPrivacyTest {
    private static final String URL =
            "https://collector.invalid/v1/metrics";
    private static final String AUTHORIZATION =
            "Bearer fictional-otlp-authorization";
    private static final String REQUEST_ID =
            "11111111-1111-4111-8111-111111111111";
    private static final String PRIVATE_FIXTURE =
            "Иван +7 999 123-45-67 fictional-private-comment";

    @Test
    void finalPublishExportsOnlyCanonicalMetersAndKeepsAuthorizationOutOfPayload()
            throws Exception {
        List<OtlpMetricsSender.Request> captured = new ArrayList<>();
        OtlpConfig config = config();
        OtlpMeterRegistry registry = OtlpMeterRegistry.builder(config)
                .clock(Clock.SYSTEM)
                .metricsSender(request -> captured.add(request))
                .build();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        new TelemetryConfiguration()
                .boundedTelemetry(environment)
                .customize(registry);

        var leads = new LeadMetrics(registry);
        leads.accepted(AcceptanceOutcome.CREATED);
        leads.rejected(LeadRejectionReason.VALIDATION);
        Counter.builder("andrew.telegram.delivery")
                .tag("outcome", "delivered")
                .tag("reason", "success")
                .register(registry)
                .increment();
        Timer.builder("andrew.telegram.client")
                .tag("method", "POST")
                .tag("uri", "/bot{token}/sendMessage")
                .tag("outcome", "delivered")
                .register(registry)
                .record(Duration.ofMillis(5));
        Counter.builder("andrew.leads.accepted")
                .tag("outcome", "created")
                .tag("requestId", REQUEST_ID)
                .register(registry)
                .increment();
        Counter.builder("rogue.metric")
                .tag("lead", PRIVATE_FIXTURE)
                .register(registry)
                .increment();

        registry.close();

        assertThat(captured).singleElement().satisfies(request -> {
            assertThat(request.getAddress()).isEqualTo(URL);
            assertThat(request.getHeaders())
                    .containsEntry("Authorization", AUTHORIZATION);
            assertThat(request.getHeaders().keySet())
                    .containsExactly("Authorization");
        });
        byte[] payload = captured.getFirst().getMetricsData();
        ExportMetricsServiceRequest export =
                ExportMetricsServiceRequest.parseFrom(payload);
        assertThat(export.getResourceMetricsList())
                .singleElement()
                .satisfies(resource -> assertCanonicalResource(
                        resource.getResource().getAttributesList()));
        List<Metric> metrics = export.getResourceMetricsList().stream()
                .flatMap(resource -> resource.getScopeMetricsList().stream())
                .flatMap(scope -> scope.getMetricsList().stream())
                .toList();

        assertThat(metrics)
                .extracting(Metric::getName)
                .containsExactlyInAnyOrder(
                        "andrew.leads.accepted",
                        "andrew.leads.rejected",
                        "andrew.telegram.client",
                        "andrew.telegram.delivery");
        assertThat(metrics).allSatisfy(
                OtlpExportPrivacyTest::assertCanonicalAttributes);
        assertThat(new String(payload, StandardCharsets.ISO_8859_1))
                .doesNotContain(
                        AUTHORIZATION,
                        URL,
                        REQUEST_ID,
                        PRIVATE_FIXTURE,
                        "fictional-private-comment");
    }

    private static OtlpConfig config() {
        OtlpMetricsProperties properties =
                new OtlpMetricsProperties();
        properties.setEnabled(true);
        properties.setUrl(URL);
        properties.setHeaders(
                Map.of("Authorization", AUTHORIZATION));
        properties.setStep(Duration.ofDays(1));
        return new TelemetryConfiguration()
                .boundedOtlpConfig(properties);
    }

    static void assertCanonicalResource(List<KeyValue> attributes) {
        Map<String, String> actual = attributes.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        KeyValue::getKey,
                        attribute ->
                                attribute.getValue().getStringValue()));
        var expected = new java.util.HashMap<>(Map.of(
                "service.name", "andrew-website",
                "telemetry.sdk.name", "io.micrometer",
                "telemetry.sdk.language", "java"));
        String sdkVersion = MeterRegistry.class.getPackage()
                .getImplementationVersion();
        if (sdkVersion != null) {
            expected.put("telemetry.sdk.version", sdkVersion);
        }
        assertThat(actual)
                .containsExactlyInAnyOrderEntriesOf(expected);
    }

    private static void assertCanonicalAttributes(Metric metric) {
        if (metric.hasSum()) {
            assertThat(metric.getSum().getDataPointsList())
                    .singleElement()
                    .satisfies(point ->
                            assertPoint(
                                    metric.getName(),
                                    point.getAttributesList()));
        } else {
            assertThat(metric.hasHistogram()).isTrue();
            assertThat(metric.getHistogram().getDataPointsList())
                    .singleElement()
                    .satisfies(point ->
                            assertPoint(
                                    metric.getName(),
                                    point.getAttributesList()));
        }
    }

    private static void assertPoint(
            String metricName,
            List<KeyValue> pointAttributes) {
        Map<String, String> attributes = pointAttributes.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        attribute -> attribute.getKey(),
                        attribute -> attribute.getValue().getStringValue()));
        assertThat(attributes)
                .containsEntry("application", "andrew-website")
                .containsEntry("profile", "test");
        if ("andrew.leads.accepted".equals(metricName)) {
            assertThat(attributes)
                    .containsEntry("outcome", "created")
                    .hasSize(3);
        } else if ("andrew.leads.rejected".equals(metricName)) {
            assertThat(attributes)
                    .containsEntry("reason", "validation")
                    .hasSize(3);
        } else if ("andrew.telegram.delivery".equals(metricName)) {
            assertThat(attributes)
                    .containsEntry("outcome", "delivered")
                    .containsEntry("reason", "success")
                    .hasSize(4);
        } else {
            assertThat(attributes)
                    .containsEntry("method", "POST")
                    .containsEntry(
                            "uri", "/bot{token}/sendMessage")
                    .containsEntry("outcome", "delivered")
                    .hasSize(5);
        }
        assertThat(attributes.keySet())
                .doesNotContainAnyElementsOf(Set.of(
                        "requestId", "http.url", "exception", "host"));
    }
}
