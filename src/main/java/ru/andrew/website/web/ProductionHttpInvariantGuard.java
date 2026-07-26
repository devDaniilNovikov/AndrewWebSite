package ru.andrew.website.web;

import java.time.Duration;
import java.util.Set;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.actuate.autoconfigure.endpoint.PropertiesEndpointAccessResolver;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.autoconfigure.web.ErrorProperties.IncludeAttribute;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointProperties;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointProperties.Group;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthProperties;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.autoconfigure.ServerProperties.ForwardHeadersStrategy;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import ru.andrew.website.common.ProductionStartupFailureReporter;

public final class ProductionHttpInvariantGuard implements EnvironmentPostProcessor, Ordered {
    public static final String MESSAGE =
            "Production HTTP configuration violates the public boundary";

    private static final EndpointId HEALTH_ENDPOINT = HealthEndpoint.ID;
    private static final Set<EndpointId> PUBLIC_ENDPOINTS = Set.of(HEALTH_ENDPOINT);
    private static final Set<String> LIVENESS_MEMBERS = Set.of("livenessState");
    private static final Set<String> READINESS_MEMBERS = Set.of(
            "readinessState", "dbReadiness", "telegramWorkerReadiness");
    private static final int CONTAINER_SERVER_PORT = 8080;
    private static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER + 3;

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.matchesProfiles("prod")) {
            return;
        }
        ProductionStartupFailureReporter
                .prepareEarlyFailure(application);

        Binder binder = Binder.get(environment);
        ServerProperties server = bind(binder, "server", ServerProperties.class);
        TomcatServerProperties tomcat =
                bind(binder, "server.tomcat", TomcatServerProperties.class);
        WebMvcProperties mvc = bind(binder, "spring.mvc", WebMvcProperties.class);
        WebEndpointProperties endpoints =
                bind(binder, "management.endpoints.web", WebEndpointProperties.class);
        CorsEndpointProperties endpointCors = bind(
                binder, "management.endpoints.web.cors", CorsEndpointProperties.class);
        org.springframework.boot.autoconfigure.web.WebProperties bootWeb = bind(
                binder, "spring.web", org.springframework.boot.autoconfigure.web.WebProperties.class);
        HealthEndpointProperties health = bind(
                binder, "management.endpoint.health", HealthEndpointProperties.class);
        WebProperties appWeb = binder.bindOrCreate("app.web", WebProperties.class);
        WebApplicationType webApplicationType = binder
                .bind("spring.main.web-application-type", WebApplicationType.class)
                .orElse(application.getWebApplicationType());

        if (webApplicationType != WebApplicationType.SERVLET
                || hasUnsafeServerBinding(server)
                || server.getForwardHeadersStrategy() != ForwardHeadersStrategy.NONE
                || StringUtils.hasText(tomcat.getRemoteip().getProtocolHeader())
                || StringUtils.hasText(tomcat.getRemoteip().getRemoteIpHeader())
                || StringUtils.hasText(server.getServlet().getContextPath())
                || !isRootServletPath(mvc.getServlet().getPath())
                || !appWeb.rateLimit().enabled()
                || !appWeb.localCorsOrigins().isEmpty()
                || !"/actuator".equals(endpoints.getBasePath())
                || hasUnsafeExposure(endpoints)
                || hasUnsafeHealthPathMapping(endpoints)
                || !endpointCors.getAllowedOrigins().isEmpty()
                || !endpointCors.getAllowedOriginPatterns().isEmpty()
                || ManagementPortType.get(environment) != ManagementPortType.SAME
                || hasUnsafeErrorHandling(bootWeb)
                || !hasReadOnlyHealthAccess(environment)
                || health.getShowDetails() != Show.NEVER
                || isPublic(health.getShowComponents())
                || hasUnsafeHealthCache(environment)
                || hasCustomStatus(health)
                || hasUnsafeGroupOverride(health)
                || !isCanonicalPublicGroup(health, "liveness", LIVENESS_MEMBERS)
                || !isCanonicalPublicGroup(health, "readiness", READINESS_MEMBERS)
                || environment.getProperty(
                        "management.endpoint.health.probes.add-additional-paths",
                        Boolean.class,
                        false)
                || !environment.getProperty(
                        "management.endpoint.health.probes.enabled",
                        Boolean.class,
                        false)) {
            throw new ApplicationContextException(MESSAGE);
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private static boolean hasUnsafeServerBinding(ServerProperties server) {
        Integer port = server.getPort();
        if (port != null && port != CONTAINER_SERVER_PORT) {
            return true;
        }
        var address = server.getAddress();
        return address != null && !"0.0.0.0".equals(address.getHostAddress());
    }

    private static boolean hasUnsafeExposure(WebEndpointProperties endpoints) {
        Set<EndpointId> included = endpointIds(endpoints.getExposure().getInclude());
        if (!PUBLIC_ENDPOINTS.equals(included)) {
            return true;
        }
        for (String excluded : endpoints.getExposure().getExclude()) {
            EndpointId endpointId = endpointIdOrNull(excluded);
            if (endpointId == null || HEALTH_ENDPOINT.equals(endpointId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasUnsafeHealthPathMapping(WebEndpointProperties endpoints) {
        for (var mapping : endpoints.getPathMapping().entrySet()) {
            EndpointId endpointId = endpointIdOrNull(mapping.getKey());
            if (endpointId == null) {
                return true;
            }
            if (HEALTH_ENDPOINT.equals(endpointId)) {
                String path = mapping.getValue();
                return StringUtils.hasText(path) && !"health".equals(path);
            }
        }
        return false;
    }

    private static Set<EndpointId> endpointIds(Set<String> values) {
        try {
            return values.stream()
                    .map(ProductionHttpInvariantGuard::endpointId)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        } catch (IllegalArgumentException invalidEndpointId) {
            return Set.of();
        }
    }

    private static EndpointId endpointId(String value) {
        if (!StringUtils.hasText(value) || isWildcard(value)) {
            throw new IllegalArgumentException("Invalid endpoint id");
        }
        return EndpointId.fromPropertyValue(value);
    }

    private static EndpointId endpointIdOrNull(String value) {
        try {
            return endpointId(value);
        } catch (IllegalArgumentException invalidEndpointId) {
            return null;
        }
    }

    private static boolean isWildcard(String value) {
        return "*".equals(value == null ? null : value.trim());
    }

    private static boolean hasUnsafeErrorHandling(
            org.springframework.boot.autoconfigure.web.WebProperties web) {
        var error = web.getError();
        return !"/error".equals(error.getPath())
                || error.isIncludeException()
                || error.getIncludeMessage() != IncludeAttribute.NEVER
                || error.getIncludeBindingErrors() != IncludeAttribute.NEVER
                || error.getIncludeStacktrace() != IncludeAttribute.NEVER
                || error.getIncludePath() != IncludeAttribute.NEVER;
    }

    private static boolean hasReadOnlyHealthAccess(Environment environment) {
        try {
            return new PropertiesEndpointAccessResolver(environment)
                            .accessFor(HealthEndpoint.ID, Access.UNRESTRICTED)
                    == Access.READ_ONLY;
        } catch (RuntimeException invalidConfiguration) {
            return false;
        }
    }

    private static boolean hasUnsafeHealthCache(Environment environment) {
        try {
            Duration timeToLive = environment.getProperty(
                    "management.endpoint.health.cache.time-to-live",
                    Duration.class,
                    Duration.ZERO);
            return !timeToLive.isZero();
        } catch (RuntimeException invalidConfiguration) {
            return true;
        }
    }

    private static boolean isRootServletPath(String path) {
        return path.isEmpty() || "/".equals(path);
    }

    private static boolean hasUnsafeGroupOverride(HealthEndpointProperties health) {
        return health.getGroup().values().stream().anyMatch(group ->
                group.getAdditionalPath() != null
                        || isPublic(group.getShowDetails())
                        || isPublic(group.getShowComponents()));
    }

    private static boolean isCanonicalPublicGroup(
            HealthEndpointProperties health, String name, Set<String> members) {
        Group group = health.getGroup().get(name);
        return group != null
                && members.equals(group.getInclude())
                && (group.getExclude() == null || group.getExclude().isEmpty())
                && !hasCustomStatus(group);
    }

    private static boolean hasCustomStatus(HealthProperties health) {
        return !health.getStatus().getOrder().isEmpty()
                || !health.getStatus().getHttpMapping().isEmpty();
    }

    private static boolean isPublic(Show visibility) {
        return visibility != null && visibility != Show.NEVER;
    }

    private static <T> T bind(Binder binder, String prefix, Class<T> type) {
        return binder.bindOrCreate(prefix, type);
    }
}
