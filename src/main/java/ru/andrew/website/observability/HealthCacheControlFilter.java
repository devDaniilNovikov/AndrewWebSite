package ru.andrew.website.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class HealthCacheControlFilter extends OncePerRequestFilter {
    private static final String ACTUATOR_ROOT = "/actuator";
    private static final String GET = "GET";
    private static final String NO_STORE = "no-store";
    private static final Set<String> PATHS = Set.of(
            "/actuator/health/liveness", "/actuator/health/readiness");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String rawPath = contextRelativePath(request);
        if (isRawActuatorCandidate(rawPath)) {
            return false;
        }
        try {
            String normalizedPath = normalizedPath(request);
            return !isActuatorPath(normalizedPath)
                    && !isActuatorPath(UriUtils.decode(normalizedPath, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException invalidEncoding) {
            return true;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String rawPath = contextRelativePath(request);
        String normalizedPath;
        try {
            normalizedPath = normalizedPath(request);
        } catch (IllegalArgumentException invalidEncoding) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!GET.equals(request.getMethod())
                || !rawPath.equals(normalizedPath)
                || !PATHS.contains(normalizedPath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        HttpServletResponse exact = new HttpServletResponseWrapper(response) {
            @Override
            public void setHeader(String name, String value) {
                super.setHeader(name, normalized(name, value));
            }

            @Override
            public void addHeader(String name, String value) {
                if (HttpHeaders.CACHE_CONTROL.equalsIgnoreCase(name)) {
                    super.setHeader(HttpHeaders.CACHE_CONTROL, NO_STORE);
                } else {
                    super.addHeader(name, value);
                }
            }

            private String normalized(String name, String value) {
                return HttpHeaders.CACHE_CONTROL.equalsIgnoreCase(name) ? NO_STORE : value;
            }
        };
        exact.setHeader(HttpHeaders.CACHE_CONTROL, NO_STORE);
        chain.doFilter(request, exact);
    }

    private boolean isActuatorPath(String path) {
        return path.equals(ACTUATOR_ROOT) || path.startsWith(ACTUATOR_ROOT + "/");
    }

    private boolean isRawActuatorCandidate(String path) {
        return isActuatorPath(path)
                || path.startsWith(ACTUATOR_ROOT + ";")
                || path.regionMatches(true, 0, ACTUATOR_ROOT + "%", 0, ACTUATOR_ROOT.length() + 1);
    }

    private String normalizedPath(HttpServletRequest request) {
        return UrlPathHelper.defaultInstance.getPathWithinApplication(request);
    }

    private String contextRelativePath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath == null || contextPath.isEmpty() || !requestUri.startsWith(contextPath)) {
            return requestUri;
        }
        return requestUri.substring(contextPath.length());
    }
}
