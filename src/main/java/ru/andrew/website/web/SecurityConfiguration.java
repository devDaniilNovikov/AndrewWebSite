package ru.andrew.website.web;

import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
class SecurityConfiguration {
    @Bean
    ClientRateLimiter clientRateLimiter(Clock clock, WebProperties properties) {
        WebProperties.RateLimit rateLimit = properties.rateLimit();
        return new ClientRateLimiter(
                clock,
                new SlidingWindowRateLimiter(
                        rateLimit.globalLimit(), rateLimit.globalWindow(), clock),
                rateLimit.maxClients(),
                rateLimit.clientIdleTtl(),
                rateLimit.clientCapacity(),
                rateLimit.clientRefill());
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, WebProperties properties,
            ClientRateLimiter limiter, ProblemResponseWriter problems,
            @Qualifier("localCorsConfigurationSource")
            ObjectProvider<CorsConfigurationSource> localCorsSource) throws Exception {
        RequestBodyLimitFilter bodyLimit = new RequestBodyLimitFilter(properties, problems);
        RateLimitFilter rateLimit = new RateLimitFilter(properties, limiter, problems);
        CorsConfigurationSource localCors = localCorsSource.getIfAvailable();

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.requestCache(cache -> cache.disable());
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());
        http.logout(logout -> logout.disable());
        if (localCors == null) {
            http.cors(cors -> cors.configurationSource(request -> null));
        } else {
            http.cors(cors -> cors.configurationSource(localCors));
        }
        http.csrf(csrf -> csrf.ignoringRequestMatchers(
                methodAndPath(HttpMethod.POST, "/api/leads")));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(methodAndPath(HttpMethod.POST, "/api/leads")).permitAll()
                .requestMatchers(
                        methodAndPath(HttpMethod.GET, "/actuator/health/liveness"),
                        methodAndPath(HttpMethod.GET, "/actuator/health/readiness")).permitAll()
                .requestMatchers(namespace("/api"), namespace("/actuator")).denyAll()
                .requestMatchers(method(HttpMethod.GET), method(HttpMethod.HEAD)).permitAll()
                .anyRequest().denyAll());
        http.addFilterBefore(rateLimit, AuthorizationFilter.class);
        http.addFilterBefore(bodyLimit, RateLimitFilter.class);
        return http.build();
    }

    private static RequestMatcher methodAndPath(HttpMethod method, String expectedPath) {
        return request -> method.matches(request.getMethod())
                && expectedPath.equals(path(request));
    }

    private static RequestMatcher namespace(String namespace) {
        return request -> path(request).equals(namespace)
                || path(request).startsWith(namespace + "/");
    }

    private static RequestMatcher method(HttpMethod method) {
        return request -> method.matches(request.getMethod());
    }

    private static String path(jakarta.servlet.http.HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
