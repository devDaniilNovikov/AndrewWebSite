package ru.andrew.website.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
final class PublicBoundaryDenyFilter extends OncePerRequestFilter {
    private static final String LEAD_PATH = "/api/leads";
    private static final String LIVENESS_PATH = "/actuator/health/liveness";
    private static final String READINESS_PATH = "/actuator/health/readiness";
    private final boolean localProfile;

    PublicBoundaryDenyFilter(Environment environment) {
        this.localProfile = environment.matchesProfiles("local");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = path(request);
        if ((localProfile && CorsUtils.isPreFlightRequest(request))
                || isAllowedBoundaryRequest(request, path)
                || !isClosedNamespace(path)) {
            chain.doFilter(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    private boolean isAllowedBoundaryRequest(HttpServletRequest request, String path) {
        return (HttpMethod.POST.matches(request.getMethod()) && LEAD_PATH.equals(path))
                || (HttpMethod.GET.matches(request.getMethod())
                        && (LIVENESS_PATH.equals(path) || READINESS_PATH.equals(path)));
    }

    private boolean isClosedNamespace(String path) {
        return isNamespace(path, "/api") || isNamespace(path, "/actuator");
    }

    private boolean isNamespace(String path, String namespace) {
        return path.equals(namespace) || path.startsWith(namespace + "/");
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
