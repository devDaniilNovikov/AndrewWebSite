package ru.andrew.website.web;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults;
import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;
import static org.springframework.security.web.util.matcher.RequestMatchers.allOf;

import jakarta.servlet.DispatcherType;
import java.time.Clock;
import java.util.regex.Pattern;
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
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import ru.andrew.website.leads.LeadMetrics;

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
    SlidingWindowRateLimiter perimeterRateLimiter(
            Clock clock, WebProperties properties) {
        WebProperties.RateLimit rateLimit = properties.rateLimit();
        return new SlidingWindowRateLimiter(
                rateLimit.maxClients(), rateLimit.globalWindow(), clock);
    }

    @Bean
    PerimeterRateLimitFilter perimeterRateLimitFilter(
            WebProperties properties,
            SlidingWindowRateLimiter perimeterRateLimiter,
            ProblemResponseWriter problems) {
        return new PerimeterRateLimitFilter(
                properties.rateLimit().enabled(),
                perimeterRateLimiter,
                problems);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, WebProperties properties,
            ClientRateLimiter limiter, ProblemResponseWriter problems,
            LeadMetrics metrics,
            @Qualifier("localCorsConfigurationSource")
            ObjectProvider<CorsConfigurationSource> localCorsSource) throws Exception {
        RequestBodyLimitFilter bodyLimit =
                new RequestBodyLimitFilter(properties, problems, metrics);
        RateLimitFilter rateLimit =
                new RateLimitFilter(properties, limiter, problems, metrics);
        CorsConfigurationSource localCors = localCorsSource.getIfAvailable();
        RequestMatcher lead = exact(HttpMethod.POST, "/api/leads");
        RequestMatcher liveness = exact(HttpMethod.GET, "/actuator/health/liveness");
        RequestMatcher readiness = exact(HttpMethod.GET, "/actuator/health/readiness");
        RequestMatcher error = exact("/error");
        RequestMatcher errorDispatch =
                allOf(new DispatcherTypeRequestMatcher(DispatcherType.ERROR), error);
        var paths = withDefaults();

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.requestCache(cache -> cache.disable());
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());
        http.logout(logout -> logout.disable());
        if (localCors == null) {
            http.cors(cors -> cors.disable());
        } else {
            http.cors(cors -> cors.configurationSource(localCors));
        }
        http.csrf(csrf -> csrf.ignoringRequestMatchers(lead));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(errorDispatch).permitAll()
                .requestMatchers(error).denyAll()
                .requestMatchers(lead).permitAll()
                .requestMatchers(liveness, readiness).permitAll()
                .requestMatchers(
                        paths.matcher("/api/**"),
                        paths.matcher("/actuator/**")).denyAll()
                .requestMatchers(
                        regexMatcher(HttpMethod.GET),
                        regexMatcher(HttpMethod.HEAD)).permitAll()
                .anyRequest().denyAll());
        http.addFilterBefore(rateLimit, AuthorizationFilter.class);
        http.addFilterBefore(bodyLimit, RateLimitFilter.class);
        return http.build();
    }

    private static RequestMatcher exact(HttpMethod method, String path) {
        return regexMatcher(method, exactPattern(path));
    }

    private static RequestMatcher exact(String path) {
        return regexMatcher(exactPattern(path));
    }

    private static String exactPattern(String path) {
        return "^" + Pattern.quote(path) + "(?:\\?.*)?$";
    }
}
