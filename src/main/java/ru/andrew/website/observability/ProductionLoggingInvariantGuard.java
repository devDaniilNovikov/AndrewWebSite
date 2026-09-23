package ru.andrew.website.observability;

import java.util.Map;
import java.util.Set;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;
import ru.andrew.website.AndrewWebsiteApplication;
import ru.andrew.website.common.ProductionStartupFailureReporter;

public final class ProductionLoggingInvariantGuard
        implements EnvironmentPostProcessor, Ordered {
    public static final String MESSAGE =
            "Production logging configuration violates the telemetry boundary";

    private static final String APPLICATION = "andrew-website";
    private static final Set<String> SAFE_ERROR_LOGGERS =
            Set.of(
                    "ru.andrew.website.common"
                            + ".ProductionStartupFailureReporter",
                    "ru.andrew.website.leads"
                            + ".TelegramLeadDelivery");
    private static final Set<String> PROTECTED_LOGGERS =
            Set.of(
                    "org.apache.catalina",
                    "org.springframework.web");
    private static final Set<String> FORBIDDEN_LOGGING_PROPERTIES =
            Set.of(
                    "logging.file",
                    "logging.file.name",
                    "logging.file.path",
                    "logging.structured.ecs.service.environment",
                    "logging.structured.ecs.service.name",
                    "logging.structured.ecs.service.node-name",
                    "logging.structured.ecs.service.version",
                    "logging.structured.format.file",
                    "logging.structured.gelf.host",
                    "logging.structured.gelf.service.version",
                    "logging.structured.json.context.prefix",
                    "logging.structured.json.stacktrace.include-common-frames",
                    "logging.structured.json.stacktrace.include-hashes",
                    "logging.structured.json.stacktrace.max-length",
                    "logging.structured.json.stacktrace"
                            + ".max-throwable-depth",
                    "logging.structured.json.stacktrace.printer",
                    "logging.structured.json.stacktrace.root");
    private static final int ORDER =
            ConfigDataEnvironmentPostProcessor.ORDER + 4;

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application) {
        if (!environment.matchesProfiles("prod")) {
            return;
        }
        ProductionStartupFailureReporter
                .prepareEarlyFailure(application);
        boolean valid;
        try {
            valid = isValid(Binder.get(environment));
        } catch (RuntimeException invalidConfiguration) {
            valid = false;
        }
        if (!valid) {
            throw new ApplicationContextException(MESSAGE);
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private static boolean isValid(Binder binder) {
        String structuredConsole = binder.bind(
                        "logging.structured.format.console",
                        String.class)
                .orElse("");
        String externalLoggingConfig = binder.bind(
                        "logging.config", String.class)
                .orElse("");
        String applicationName = binder.bind(
                        "spring.application.name", String.class)
                .orElse("");
        String applicationVersion = binder.bind(
                        "spring.application.version", String.class)
                .orElse(null);
        String bannerMode = binder.bind(
                        "spring.main.banner-mode", String.class)
                .orElse("");
        boolean logsStartupInfo = binder.bind(
                        "spring.main.log-startup-info", Boolean.class)
                .orElse(true);
        boolean includesStructuredContext = binder.bind(
                        "logging.structured.json.context.include",
                        Boolean.class)
                .orElse(true);
        Map<String, String> loggingLevels = binder.bind(
                        "logging.level",
                        Bindable.mapOf(String.class, String.class))
                .orElse(Map.of());
        Map<String, String> loggingGroups = binder.bind(
                        "logging.group",
                        Bindable.mapOf(String.class, String.class))
                .orElse(Map.of());
        boolean debug = binder.bind("debug", Boolean.class)
                .orElse(false);
        boolean trace = binder.bind("trace", Boolean.class)
                .orElse(false);

        return "ecs".equals(structuredConsole)
                && !StringUtils.hasText(externalLoggingConfig)
                && APPLICATION.equals(applicationName)
                && isSafeApplicationVersion(
                        applicationVersion,
                        AndrewWebsiteApplication.class.getPackage()
                                .getImplementationVersion())
                && "off".equalsIgnoreCase(bannerMode)
                && !logsStartupInfo
                && !includesStructuredContext
                && loggingGroups.isEmpty()
                && !debug
                && !trace
                && hasNoUnsafeStructuredOverrides(binder)
                && hasSafeLoggingLevels(loggingLevels);
    }

    static boolean isSafeApplicationVersion(
            String configured,
            String packaged) {
        return configured == null
                || StringUtils.hasText(packaged)
                && packaged.equals(configured);
    }

    private static boolean hasSafeLoggingLevels(
            Map<String, String> levels) {
        for (String protectedLogger : PROTECTED_LOGGERS) {
            if (!"OFF".equals(levels.get(protectedLogger))) {
                return false;
            }
        }
        if (!"OFF".equals(levels.get("root"))
                || SAFE_ERROR_LOGGERS.stream().anyMatch(
                        logger -> !"ERROR".equals(
                                levels.get(logger)))) {
            return false;
        }
        return levels.entrySet().stream()
                .allMatch(entry -> {
                    if ("root".equals(entry.getKey())) {
                        return "OFF".equals(entry.getValue());
                    }
                    if (SAFE_ERROR_LOGGERS.contains(entry.getKey())) {
                        return "ERROR".equals(entry.getValue());
                    }
                    return "OFF".equals(entry.getValue());
                });
    }

    private static boolean hasNoUnsafeStructuredOverrides(
            Binder binder) {
        boolean scalarOverride =
                FORBIDDEN_LOGGING_PROPERTIES.stream()
                        .anyMatch(property -> binder.bind(
                                property, String.class).isBound());
        Map<String, String> addedMembers = binder.bind(
                        "logging.structured.json.add",
                        Bindable.mapOf(String.class, String.class))
                .orElse(Map.of());
        Map<String, String> renamedMembers = binder.bind(
                        "logging.structured.json.rename",
                        Bindable.mapOf(String.class, String.class))
                .orElse(Map.of());
        Set<String> includedMembers = binder.bind(
                        "logging.structured.json.include",
                        Bindable.setOf(String.class))
                .orElse(Set.of());
        Set<String> excludedMembers = binder.bind(
                        "logging.structured.json.exclude",
                        Bindable.setOf(String.class))
                .orElse(Set.of());
        Set<String> customizers = binder.bind(
                        "logging.structured.json.customizer",
                        Bindable.setOf(String.class))
                .orElse(Set.of());
        return !scalarOverride
                && addedMembers.isEmpty()
                && renamedMembers.isEmpty()
                && includedMembers.isEmpty()
                && excludedMembers.isEmpty()
                && customizers.isEmpty();
    }
}
