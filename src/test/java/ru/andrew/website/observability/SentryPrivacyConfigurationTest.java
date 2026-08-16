package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.Breadcrumb;
import io.sentry.CustomSamplingContext;
import io.sentry.Hint;
import io.sentry.SamplingContext;
import io.sentry.SentryEvent;
import io.sentry.SentryLogEvent;
import io.sentry.SentryLogEventAttributeValue;
import io.sentry.SentryLogLevel;
import io.sentry.SentryMetricsEvent;
import io.sentry.SentryOptions;
import io.sentry.SpanContext;
import io.sentry.SpanStatus;
import io.sentry.TransactionContext;
import io.sentry.protocol.DebugMeta;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.SentryId;
import io.sentry.protocol.SentryStackFrame;
import io.sentry.protocol.SentryStackTrace;
import io.sentry.protocol.SdkVersion;
import io.sentry.protocol.SentryThread;
import io.sentry.protocol.SentryTransaction;
import io.sentry.protocol.TransactionInfo;
import io.sentry.protocol.TransactionNameSource;
import io.sentry.protocol.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class SentryPrivacyConfigurationTest {
    private static final String PRIVATE = "fictional-private-value";

    private final SentryPrivacyConfiguration configuration =
            new SentryPrivacyConfiguration();

    @Test
    void errorCallbackRetainsOnlyRedactedExceptionAndSafeFrames() {
        SentryEvent event = new SentryEvent(new IllegalStateException(PRIVATE));
        event.setRequest(new Request());
        event.setUser(new User());
        event.setBreadcrumbs(List.of(new Breadcrumb(PRIVATE)));
        event.setExtras(Map.of("private", PRIVATE));
        event.setTags(Map.of("private", PRIVATE));
        Message message = new Message();
        message.setMessage(PRIVATE);
        event.setMessage(message);
        event.setThreads(List.of(new SentryThread()));
        SentryException exception = new SentryException();
        exception.setType("IllegalStateException");
        exception.setValue(PRIVATE);
        exception.setModule(PRIVATE);
        exception.setThreadId(42L);
        exception.setUnknown(Map.of("private", PRIVATE));
        SentryStackFrame frame = new SentryStackFrame();
        frame.setModule("ru.andrew.website.SafeClass");
        frame.setFunction("safeMethod");
        frame.setFilename("SafeClass.java");
        frame.setLineno(42);
        frame.setVars(Map.of("private", PRIVATE));
        frame.setContextLine(PRIVATE);
        frame.setPreContext(List.of(PRIVATE));
        frame.setPostContext(List.of(PRIVATE));
        frame.setAbsPath("/private/" + PRIVATE);
        frame.setPackage(PRIVATE);
        frame.setRawFunction(PRIVATE);
        frame.setUnknown(Map.of("private", PRIVATE));
        SentryStackTrace stackTrace = new SentryStackTrace(List.of(frame));
        stackTrace.setSnapshot(true);
        stackTrace.setInstructionAddressAdjustment(
                SentryStackTrace.InstructionAddressAdjustment.ALL);
        exception.setStacktrace(stackTrace);
        event.setExceptions(List.of(exception));
        event.getContexts().put("private", Map.of("value", PRIVATE));
        event.setRelease(PRIVATE);
        event.setEnvironment(PRIVATE);
        event.setPlatform(PRIVATE);
        event.setServerName(PRIVATE);
        event.setDist(PRIVATE);
        event.setSdk(new SdkVersion(PRIVATE, PRIVATE));
        event.setDebugMeta(new DebugMeta());
        event.setUnknown(Map.of("private", PRIVATE));

        SentryEvent sanitized = configuration.beforeSend()
                .execute(event, new Hint());

        assertThat(sanitized).isSameAs(event);
        assertThat(event.getRequest()).isNull();
        assertThat(event.getUser()).isNull();
        assertThat(event.getBreadcrumbs()).isNull();
        assertThat(event.getExtras()).isNull();
        assertThat(event.getTags()).isNull();
        assertThat(event.getMessage()).isNull();
        assertThat(event.getThrowable()).isNull();
        assertThat(event.getRelease()).isNull();
        assertThat(event.getDist()).isNull();
        assertThat(event.getSdk()).isNull();
        assertThat(event.getDebugMeta()).isNull();
        assertThat(event.getEnvironment()).isEqualTo("prod");
        assertThat(event.getPlatform()).isEqualTo("java");
        assertThat(event.getServerName()).isEqualTo("andrew-website");
        assertThat(event.getThreads()).isEmpty();
        assertThat(event.getContexts().isEmpty()).isTrue();
        assertThat(event.getUnknown()).isNull();
        assertThat(event.getExceptions()).singleElement().satisfies(redacted -> {
            assertThat(redacted.getType()).isEqualTo("IllegalStateException");
            assertThat(redacted.getValue())
                    .isEqualTo(SentryPrivacyConfiguration.REDACTED_EXCEPTION_VALUE);
            assertThat(redacted.getThreadId()).isNull();
            assertThat(redacted.getMechanism()).isNull();
            assertThat(redacted.getModule()).isNull();
            assertThat(redacted.getUnknown()).isNull();
            assertThat(redacted.getStacktrace().getSnapshot()).isNull();
            assertThat(redacted.getStacktrace()
                            .getInstructionAddressAdjustment())
                    .isNull();
            assertThat(redacted.getStacktrace().getFrames())
                    .singleElement()
                    .satisfies(safeFrame -> {
                        assertThat(safeFrame.getModule())
                                .isEqualTo("ru.andrew.website.SafeClass");
                        assertThat(safeFrame.getFunction())
                                .isEqualTo("safeMethod");
                        assertThat(safeFrame.getFilename())
                                .isEqualTo("SafeClass.java");
                        assertThat(safeFrame.getLineno()).isEqualTo(42);
                        assertThat(safeFrame.getVars()).isNull();
                        assertThat(safeFrame.getContextLine()).isNull();
                        assertThat(safeFrame.getPreContext()).isNull();
                        assertThat(safeFrame.getPostContext()).isNull();
                        assertThat(safeFrame.getAbsPath()).isNull();
                        assertThat(safeFrame.getPackage()).isNull();
                        assertThat(safeFrame.getRawFunction()).isNull();
                        assertThat(safeFrame.getUnknown()).isNull();
                    });
        });
    }

    @Test
    void errorCallbackDropsMessageOnlyEvents() {
        SentryEvent event = new SentryEvent();
        Message message = new Message();
        message.setMessage(PRIVATE);
        event.setMessage(message);

        assertThat(configuration.beforeSend().execute(event, new Hint()))
                .isNull();
    }

    @Test
    void errorCallbackDropsEventsWithNoStructuredException() {
        SentryEvent event = new SentryEvent();
        event.setExceptions(List.of());

        assertThat(configuration.beforeSend().execute(event, new Hint()))
                .isNull();
    }

    @Test
    void errorCallbackHandlesMissingExceptionStackData() {
        SentryException withoutStacktrace = new SentryException();
        withoutStacktrace.setType("IllegalStateException");
        withoutStacktrace.setValue(PRIVATE);
        SentryException withoutFrames = new SentryException();
        withoutFrames.setType("IllegalArgumentException");
        withoutFrames.setValue(PRIVATE);
        withoutFrames.setStacktrace(new SentryStackTrace());
        SentryEvent event = new SentryEvent();
        event.setExceptions(List.of(withoutStacktrace, withoutFrames));

        SentryEvent sanitized = configuration.beforeSend()
                .execute(event, new Hint());

        assertThat(sanitized).isSameAs(event);
        assertThat(withoutStacktrace.getValue())
                .isEqualTo(SentryPrivacyConfiguration.REDACTED_EXCEPTION_VALUE);
        assertThat(withoutStacktrace.getStacktrace()).isNull();
        assertThat(withoutFrames.getStacktrace().getFrames()).isNull();
        assertThat(withoutFrames.getStacktrace().getUnknown()).isNull();
    }

    @Test
    void transactionCallbackAllowsOnlyCanonicalRoutesAndRemovesPayloadData() {
        SentryOptions.BeforeSendTransactionCallback callback =
                configuration.beforeSendTransaction();
        for (String name : SentryPrivacyConfiguration.SAFE_TRANSACTIONS) {
            SentryTransaction transaction = transaction(name);
            transaction.setRequest(new Request());
            transaction.setUser(new User());
            transaction.setBreadcrumbs(List.of(new Breadcrumb(PRIVATE)));
            transaction.setExtras(Map.of("private", PRIVATE));
            transaction.setTags(Map.of("private", PRIVATE));
            SpanContext originalTrace = new SpanContext("private-operation");
            originalTrace.setStatus(SpanStatus.OK);
            originalTrace.setTag("private", PRIVATE);
            originalTrace.setData("private", PRIVATE);
            originalTrace.setDescription(PRIVATE);
            originalTrace.setUnknown(Map.of("private", PRIVATE));
            originalTrace.setProfilerId(new SentryId());
            transaction.getContexts().setTrace(originalTrace);
            transaction.getContexts().put("private", Map.of("value", PRIVATE));
            transaction.setUnknown(Map.of("private", PRIVATE));
            transaction.getMeasurements().put(
                    "private",
                    new io.sentry.protocol.MeasurementValue(1, null));

            SentryTransaction sanitized = callback.execute(
                    transaction, new Hint());

            assertThat(sanitized).isSameAs(transaction);
            assertThat(transaction.getRequest()).isNull();
            assertThat(transaction.getUser()).isNull();
            assertThat(transaction.getBreadcrumbs()).isNull();
            assertThat(transaction.getExtras()).isNull();
            assertThat(transaction.getTags()).isNull();
            assertThat(transaction.getThrowable()).isNull();
            assertThat(transaction.getContexts().entrySet())
                    .extracting(Map.Entry::getKey)
                    .containsOnly("trace");
            SpanContext sanitizedTrace = transaction.getContexts().getTrace();
            assertThat(sanitizedTrace.getTraceId())
                    .isEqualTo(originalTrace.getTraceId());
            assertThat(sanitizedTrace.getSpanId())
                    .isEqualTo(originalTrace.getSpanId());
            assertThat(sanitizedTrace.getProfilerId())
                    .isEqualTo(originalTrace.getProfilerId());
            assertThat(sanitizedTrace.getOperation()).isEqualTo("http.server");
            assertThat(sanitizedTrace.getStatus()).isEqualTo(SpanStatus.OK);
            assertThat(sanitizedTrace.getDescription()).isNull();
            assertThat(sanitizedTrace.getTags()).isEmpty();
            assertThat(sanitizedTrace.getData()).isEmpty();
            assertThat(sanitizedTrace.getUnknown()).isNull();
            assertThat(transaction.getUnknown()).isNull();
            assertThat(transaction.getMeasurements()).isEmpty();
            assertThat(transaction.getSpans()).isEmpty();
        }
        assertThat(callback.execute(transaction("GET /private/raw"), new Hint()))
                .isNull();
        assertThat(callback.execute(transaction("/api/leads"), new Hint()))
                .isNull();
    }

    @Test
    void traceSamplerStartsTracingAndProfilingOnlyForCanonicalHttpRoutes() {
        SentryOptions.TracesSamplerCallback sampler =
                configuration.sentryTracesSampler();
        for (String route : SentryPrivacyConfiguration.SAFE_TRANSACTIONS) {
            int separator = route.indexOf(' ');
            MockHttpServletRequest request = new MockHttpServletRequest(
                    route.substring(0, separator),
                    route.substring(separator + 1));

            assertThat(sampler.sample(samplingContext(request)))
                    .isEqualTo(0.10D);
        }

        assertThat(sampler.sample(samplingContext(
                new MockHttpServletRequest("GET", "/private/raw"))))
                .isZero();
        assertThat(sampler.sample(samplingContext(null))).isZero();
        assertThat(sampler.sample(null)).isZero();
        assertThat(sampler.sample(new SamplingContext(
                new TransactionContext("raw", "http.server"), null)))
                .isZero();
    }

    @Test
    void transactionNameProviderAlwaysReturnsABoundedFinishableName() {
        var provider = configuration.sentryTransactionNameProvider();

        assertThat(provider.provideTransactionName(
                new MockHttpServletRequest("POST", "/api/leads")))
                .isEqualTo("POST /api/leads");
        assertThat(provider.provideTransactionName(
                new MockHttpServletRequest("GET", "/private/raw")))
                .isEqualTo(SentryPrivacyConfiguration.UNTRACKED_TRANSACTION);
    }

    @Test
    void breadcrumbAndLogCallbacksAllowOnlyTheFixedSafeLog() {
        assertThat(configuration.beforeBreadcrumb()
                .execute(new Breadcrumb(PRIVATE), new Hint()))
                .isNull();
        SentryLogEvent allowed = new SentryLogEvent(
                new SentryId(),
                0D,
                SentryPrivacyConfiguration.STARTUP_LOG,
                SentryLogLevel.INFO);
        allowed.setAttributes(Map.of(
                "private",
                new SentryLogEventAttributeValue("string", PRIVATE)));
        allowed.setUnknown(Map.of("private", PRIVATE));
        assertThat(configuration.beforeSendLog().execute(allowed))
                .isSameAs(allowed);
        assertThat(allowed.getAttributes()).isNull();
        assertThat(allowed.getUnknown()).isNull();
        assertThat(allowed.getSpanId()).isNull();
        assertThat(configuration.beforeSendLog().execute(new SentryLogEvent(
                new SentryId(), 0D, PRIVATE, SentryLogLevel.INFO)))
                .isNull();
        assertThat(configuration.beforeSendLog().execute(new SentryLogEvent(
                new SentryId(),
                0D,
                SentryPrivacyConfiguration.STARTUP_LOG,
                SentryLogLevel.ERROR)))
                .isNull();
    }

    @Test
    void metricCallbackAllowsOnlyTheFixedStartupCounter() {
        SentryMetricsEvent allowed = new SentryMetricsEvent(
                new SentryId(),
                0D,
                SentryPrivacyConfiguration.STARTUP_METRIC,
                "counter",
                1D);
        allowed.setAttributes(Map.of(
                "private",
                new SentryLogEventAttributeValue("string", PRIVATE)));
        allowed.setUnknown(Map.of("private", PRIVATE));
        assertThat(configuration.beforeSendMetric()
                .execute(allowed, new Hint())).isSameAs(allowed);
        assertThat(allowed.getAttributes()).isNull();
        assertThat(allowed.getUnknown()).isNull();
        assertThat(allowed.getSpanId()).isNull();
        assertThat(configuration.beforeSendMetric().execute(
                new SentryMetricsEvent(
                        new SentryId(), 0D, PRIVATE, "counter", 1D),
                new Hint())).isNull();
        assertThat(configuration.beforeSendMetric().execute(
                new SentryMetricsEvent(
                        new SentryId(),
                        0D,
                        SentryPrivacyConfiguration.STARTUP_METRIC,
                        "distribution",
                        1D),
                new Hint())).isNull();
        assertThat(configuration.beforeSendMetric().execute(
                new SentryMetricsEvent(
                        new SentryId(),
                        0D,
                        SentryPrivacyConfiguration.STARTUP_METRIC,
                        "counter",
                        2D),
                new Hint())).isNull();
        SentryMetricsEvent withUnit = new SentryMetricsEvent(
                new SentryId(),
                0D,
                SentryPrivacyConfiguration.STARTUP_METRIC,
                "counter",
                1D);
        withUnit.setUnit("second");
        assertThat(configuration.beforeSendMetric()
                .execute(withUnit, new Hint())).isNull();
    }

    private static SentryTransaction transaction(String name) {
        return new SentryTransaction(
                name,
                0D,
                1D,
                new ArrayList<>(),
                new HashMap<>(),
                new TransactionInfo(TransactionNameSource.ROUTE.apiName()));
    }

    private static SamplingContext samplingContext(
            MockHttpServletRequest request) {
        CustomSamplingContext custom = new CustomSamplingContext();
        if (request != null) {
            custom.set("request", request);
        }
        return new SamplingContext(
                new TransactionContext("raw", "http.server"),
                custom);
    }
}
