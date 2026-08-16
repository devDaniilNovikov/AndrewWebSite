package ru.andrew.website.observability;

import io.sentry.SentryBaseEvent;
import io.sentry.SentryLogLevel;
import io.sentry.SentryOptions;
import io.sentry.SamplingContext;
import io.sentry.SpanContext;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.SentryStackFrame;
import io.sentry.protocol.SentryStackTrace;
import io.sentry.spring7.tracing.TransactionNameProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class SentryPrivacyConfiguration {
    private static final String APPLICATION = "andrew-website";
    private static final String ENVIRONMENT = "prod";
    private static final String PLATFORM = "java";
    private static final String HTTP_SERVER_OPERATION = "http.server";
    private static final double TRACE_SAMPLE_RATE = 0.10D;

    static final String REDACTED_EXCEPTION_VALUE = "[redacted]";
    static final String STARTUP_LOG = "andrew.application.ready";
    static final String STARTUP_METRIC = "andrew.application.startup";
    static final String UNTRACKED_TRANSACTION = "untracked";
    static final Set<String> SAFE_TRANSACTIONS = Set.of(
            "POST /api/leads",
            "GET /actuator/health/liveness",
            "GET /actuator/health/readiness");

    @Bean
    SentryOptions.BeforeSendCallback beforeSend() {
        return (event, hint) -> {
            sanitizeBaseEvent(event, false);
            event.setMessage(null);
            event.setLogger(null);
            event.setThreads(null);
            event.setFingerprints(null);
            event.setModules(null);
            event.setTransaction(null);
            event.setUnknown(null);
            if (event.getExceptions() == null
                    || event.getExceptions().isEmpty()) {
                return null;
            }
            event.getExceptions().forEach(
                    SentryPrivacyConfiguration::redactException);
            return event;
        };
    }

    @Bean
    TransactionNameProvider sentryTransactionNameProvider() {
        return request -> {
            String candidate = request.getMethod() + " " + request.getRequestURI();
            return SAFE_TRANSACTIONS.contains(candidate)
                    ? candidate
                    : UNTRACKED_TRANSACTION;
        };
    }

    @Bean
    SentryOptions.TracesSamplerCallback sentryTracesSampler() {
        return context -> sampleCanonicalHttpRoute(
                context, TRACE_SAMPLE_RATE);
    }

    static double sampleCanonicalHttpRoute(
            SamplingContext context, double acceptedRate) {
        if (context == null || context.getCustomSamplingContext() == null) {
            return 0D;
        }
        Object request = context.getCustomSamplingContext().get("request");
        if (!(request instanceof HttpServletRequest httpRequest)) {
            return 0D;
        }
        String candidate = httpRequest.getMethod()
                + " "
                + httpRequest.getRequestURI();
        return SAFE_TRANSACTIONS.contains(candidate) ? acceptedRate : 0D;
    }

    @Bean
    SentryOptions.BeforeSendTransactionCallback beforeSendTransaction() {
        return (transaction, hint) -> {
            if (!SAFE_TRANSACTIONS.contains(transaction.getTransaction())) {
                return null;
            }
            sanitizeBaseEvent(transaction, true);
            transaction.getSpans().clear();
            transaction.getMeasurements().clear();
            transaction.setUnknown(null);
            return transaction;
        };
    }

    @Bean
    SentryOptions.BeforeBreadcrumbCallback beforeBreadcrumb() {
        return (breadcrumb, hint) -> null;
    }

    @Bean
    SentryOptions.Logs.BeforeSendLogCallback beforeSendLog() {
        return event -> {
            if (!STARTUP_LOG.equals(event.getBody())
                    || event.getLevel() != SentryLogLevel.INFO) {
                return null;
            }
            event.setAttributes(null);
            event.setSpanId(null);
            event.setUnknown(null);
            return event;
        };
    }

    @Bean
    SentryOptions.Metrics.BeforeSendMetricCallback beforeSendMetric() {
        return (metric, hint) -> {
            if (!STARTUP_METRIC.equals(metric.getName())
                    || !"counter".equals(metric.getType())
                    || Double.compare(metric.getValue(), 1D) != 0
                    || metric.getUnit() != null) {
                return null;
            }
            metric.setAttributes(null);
            metric.setSpanId(null);
            metric.setUnknown(null);
            return metric;
        };
    }

    private static void sanitizeBaseEvent(
            SentryBaseEvent event, boolean retainTraceIdentity) {
        SpanContext originalTrace = retainTraceIdentity
                ? event.getContexts().getTrace()
                : null;
        SpanContext trace = sanitizeTraceIdentity(originalTrace);
        event.getContexts().clear();
        if (trace != null) {
            event.getContexts().setTrace(trace);
        }
        event.setRequest(null);
        event.setUser(null);
        event.setBreadcrumbs(null);
        event.setExtras(null);
        event.setTags(null);
        event.setThrowable(null);
        event.setRelease(null);
        event.setDist(null);
        event.setSdk(null);
        event.setDebugMeta(null);
        event.setEnvironment(ENVIRONMENT);
        event.setPlatform(PLATFORM);
        event.setServerName(APPLICATION);
    }

    private static SpanContext sanitizeTraceIdentity(SpanContext trace) {
        if (trace == null) {
            return null;
        }
        SpanContext sanitized = new SpanContext(
                trace.getTraceId(),
                trace.getSpanId(),
                trace.getParentSpanId(),
                HTTP_SERVER_OPERATION,
                null,
                null,
                trace.getStatus(),
                null);
        sanitized.getTags().clear();
        sanitized.getData().clear();
        sanitized.setUnknown(null);
        sanitized.setProfilerId(trace.getProfilerId());
        return sanitized;
    }

    private static void redactException(SentryException exception) {
        exception.setValue(REDACTED_EXCEPTION_VALUE);
        exception.setModule(null);
        exception.setThreadId(null);
        exception.setMechanism(null);
        exception.setUnknown(null);
        sanitizeStackTrace(exception.getStacktrace());
    }

    private static void sanitizeStackTrace(SentryStackTrace stackTrace) {
        if (stackTrace == null) {
            return;
        }
        stackTrace.setRegisters(null);
        stackTrace.setSnapshot(null);
        stackTrace.setInstructionAddressAdjustment(null);
        stackTrace.setUnknown(null);
        if (stackTrace.getFrames() != null) {
            stackTrace.getFrames().forEach(
                    SentryPrivacyConfiguration::sanitizeStackFrame);
        }
    }

    private static void sanitizeStackFrame(SentryStackFrame frame) {
        frame.setVars(null);
        frame.setPreContext(null);
        frame.setPostContext(null);
        frame.setContextLine(null);
        frame.setAbsPath(null);
        frame.setPackage(null);
        frame.setRawFunction(null);
        frame.setPlatform(null);
        frame.setImageAddr(null);
        frame.setSymbolAddr(null);
        frame.setInstructionAddr(null);
        frame.setAddrMode(null);
        frame.setSymbol(null);
        frame.setLock(null);
        frame.setFramesOmitted(null);
        frame.setUnknown(null);
    }
}
