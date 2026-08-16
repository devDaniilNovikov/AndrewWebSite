package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import io.sentry.IScopes;
import io.sentry.NoOpContinuousProfiler;
import io.sentry.ProfileLifecycle;
import io.sentry.SentryItemType;
import io.sentry.SentryOptions.RequestSize;
import io.sentry.spring.boot4.SentryLogbackInitializer;
import io.sentry.spring.boot4.SentryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.andrew.website.leads.AcceptanceOutcome;
import ru.andrew.website.leads.LeadAcceptanceTransaction;

@SpringBootTest(properties = {
        NO_DATABASE,
        "LEAD_FINGERPRINT_HMAC_KEY="
                + "fictional-production-fingerprint-key-material-0001",
        "TELEGRAM_BOT_TOKEN=fictional-telegram-token",
        "TELEGRAM_CHAT_ID=fictional-telegram-chat",
        "OTLP_METRICS_URL=https://collector.invalid/v1/metrics",
        "OTLP_AUTHORIZATION=Bearer fictional-otlp-authorization",
        "SENTRY_DSN=https://publickey@o1.ingest.sentry.io/1",
        "test.sentry.capture-transport=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Import(ProductionTelemetryIntegrationTest.CaptureConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SentryIntegrationTest {
    @Autowired
    SentryProperties options;

    @Autowired
    IScopes scopes;

    @Autowired
    SentryTestTransportConfiguration.SentryTestTransportFactory transport;

    @Autowired
    ObjectProvider<SentryLogbackInitializer> logbackInitializer;

    @Autowired
    MockMvc mvc;

    @Autowired
    ProductionTelemetryIntegrationTest.CapturingSender otlpSender;

    @MockitoBean
    LeadAcceptanceTransaction transaction;

    @Test
    void productionStarterUsesTheCanonicalSafeConfiguration() {
        assertThat(options.isEnabled()).isTrue();
        assertThat(options.getEnvironment()).isEqualTo("prod");
        assertThat(options.getServerName()).isEqualTo("andrew-website");
        assertThat(options.getRelease()).isNull();
        assertThat(options.isEnableExternalConfiguration()).isFalse();
        assertThat(options.isUseGitCommitIdAsRelease()).isFalse();
        assertThat(options.isSendModules()).isFalse();
        assertThat(options.isAttachStacktrace()).isFalse();
        assertThat(options.isAttachThreads()).isFalse();
        assertThat(options.isPrintUncaughtStackTrace()).isFalse();
        assertThat(options.isSendClientReports()).isFalse();
        assertThat(options.isEnableAutoSessionTracking()).isFalse();
        assertThat(options.isCaptureOpenTelemetryEvents()).isFalse();
        assertThat(options.isEnableDatabaseTransactionTracing()).isFalse();
        assertThat(options.isEnableCacheTracing()).isFalse();
        assertThat(options.isEnableQueueTracing()).isFalse();
        assertThat(options.isTraceOptionsRequests()).isFalse();
        assertThat(options.isPropagateTraceparent()).isFalse();
        assertThat(options.getTracePropagationTargets()).isEmpty();
        assertThat(options.isEnableAppStartProfiling()).isFalse();
        assertThat(options.isStartProfilerOnAppStart()).isFalse();
        assertThat(options.isEnableLegacyProfiling()).isFalse();
        assertThat(options.getProfilesSampleRate()).isNull();
        assertThat(options.getSampleRate()).isEqualTo(1.0D);
        assertThat(options.isSendDefaultPii()).isFalse();
        assertThat(options.getMaxRequestBodySize()).isEqualTo(RequestSize.NONE);
        assertThat(options.isDebug()).isFalse();
        assertThat(options.isEnableSpotlight()).isFalse();
        assertThat(options.isEnablePrettySerializationOutput()).isFalse();
        assertThat(options.getTracesSampleRate()).isEqualTo(0.10D);
        assertThat(options.getTracesSampler()).isNotNull();
        assertThat(options.getProfileSessionSampleRate()).isEqualTo(1.0D);
        assertThat(options.getProfileLifecycle()).isEqualTo(ProfileLifecycle.TRACE);
        assertThat(options.getLogs().isEnabled()).isTrue();
        assertThat(options.getMetrics().isEnabled()).isTrue();
        assertThat(options.getLogging().isEnabled()).isFalse();
        assertThat(options.getMaxBreadcrumbs()).isZero();
        assertThat(options.isStrictTraceContinuation()).isTrue();
        assertThat(options.getContinuousProfiler())
                .isNotSameAs(NoOpContinuousProfiler.getInstance());
        assertThat(logbackInitializer.getIfAvailable()).isNull();
        assertThat(otlpSender).isNotNull();
    }

    @Test
    void readyEventProducesExactlyOneSanitizedLogAndMetricEnvelope() {
        scopes.flush(5_000);

        assertThat(transport.logs()).singleElement().satisfies(log -> {
            assertThat(log.getBody())
                    .isEqualTo(SentryPrivacyConfiguration.STARTUP_LOG);
            assertThat(log.getAttributes()).isNull();
            assertThat(log.getSpanId()).isNull();
        });
        assertThat(transport.metrics()).singleElement().satisfies(metric -> {
            assertThat(metric.getName())
                    .isEqualTo(SentryPrivacyConfiguration.STARTUP_METRIC);
            assertThat(metric.getType()).isEqualTo("counter");
            assertThat(metric.getValue()).isEqualTo(1D);
            assertThat(metric.getAttributes()).isNull();
            assertThat(metric.getSpanId()).isNull();
        });
    }

    @Test
    void capturedErrorEnvelopeKeepsActionableFramesWithoutSensitiveContent() {
        String privateMessage = "fictional-sensitive-exception-message";

        scopes.captureException(new IllegalStateException(privateMessage));
        scopes.flush(5_000);

        assertThat(transport.events()).singleElement().satisfies(event -> {
            assertThat(event.getRequest()).isNull();
            assertThat(event.getUser()).isNull();
            assertThat(event.getBreadcrumbs()).isNull();
            assertThat(event.getExtras()).isNull();
            assertThat(event.getTags()).isNull();
            assertThat(event.getMessage()).isNull();
            assertThat(event.getThreads()).isNullOrEmpty();
            assertThat(event.getExceptions()).singleElement().satisfies(exception -> {
                assertThat(exception.getType()).isEqualTo("IllegalStateException");
                assertThat(exception.getValue())
                        .isEqualTo(
                                SentryPrivacyConfiguration
                                        .REDACTED_EXCEPTION_VALUE);
                assertThat(exception.getStacktrace()).isNotNull();
                assertThat(exception.getStacktrace().getFrames())
                        .isNotEmpty()
                        .allSatisfy(frame -> {
                            assertThat(frame.getVars()).isNull();
                            assertThat(frame.getContextLine()).isNull();
                            assertThat(frame.getPreContext()).isNull();
                            assertThat(frame.getPostContext()).isNull();
                            assertThat(frame.getAbsPath()).isNull();
                            assertThat(frame.getPackage()).isNull();
                            assertThat(frame.getRawFunction()).isNull();
                        });
            });
        });
        assertThat(transport.serializedPayload())
                .doesNotContain(privateMessage);
    }

    @Test
    void starterExportsOnlyCanonicalSanitizedRouteTransactions()
            throws Exception {
        when(transaction.accept(any(), any()))
                .thenReturn(AcceptanceOutcome.CREATED);
        mvc.perform(post("/api/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"requestId":"77777777-7777-4777-8777-777777777777",
                         "name":"Test","phone":"+79991234567",
                         "sourcePath":"/service/","intent":"repair",
                         "consent":true,"website":""}
                        """));
        mvc.perform(get("/actuator/health/liveness"));
        mvc.perform(get("/actuator/health/readiness"));
        scopes.flush(5_000);

        assertThat(transport.transactions())
                .extracting(io.sentry.protocol.SentryTransaction::getTransaction)
                .containsExactlyInAnyOrderElementsOf(
                        SentryPrivacyConfiguration.SAFE_TRANSACTIONS);
        assertThat(transport.transactions()).allSatisfy(exported -> {
            assertThat(exported.getRequest()).isNull();
            assertThat(exported.getUser()).isNull();
            assertThat(exported.getBreadcrumbs()).isNull();
            assertThat(exported.getExtras()).isNull();
            assertThat(exported.getTags()).isNull();
            assertThat(exported.getSpans()).isEmpty();
            assertThat(exported.getMeasurements()).isEmpty();
            assertThat(exported.getContexts().entrySet())
                    .extracting(java.util.Map.Entry::getKey)
                    .containsOnly("trace");
        });

        int envelopeBaseline = transport.envelopeCount();
        mvc.perform(get("/not-a-canonical-route"));
        scopes.flush(5_000);

        assertThat(transport.itemTypesSince(envelopeBaseline))
                .doesNotContain(
                        SentryItemType.Transaction,
                        SentryItemType.Profile,
                        SentryItemType.ProfileChunk,
                        SentryItemType.Span);
        assertThat(transport.transactions())
                .extracting(io.sentry.protocol.SentryTransaction::getTransaction)
                .doesNotContain("GET /not-a-canonical-route");
    }
}
