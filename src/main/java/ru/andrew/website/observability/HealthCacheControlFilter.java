package ru.andrew.website.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class HealthCacheControlFilter extends OncePerRequestFilter {
    private static final String NO_STORE = "no-store";
    private static final Set<String> PATHS = Set.of(
            "/actuator/health/liveness", "/actuator/health/readiness");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
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
}
