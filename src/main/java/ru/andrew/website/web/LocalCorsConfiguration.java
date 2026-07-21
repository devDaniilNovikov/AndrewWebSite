package ru.andrew.website.web;

import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@Profile("local")
class LocalCorsConfiguration {
    @Bean
    CorsConfigurationSource localCorsConfigurationSource(WebProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.localCorsOrigins().stream()
                .map(Object::toString)
                .toList());
        configuration.setAllowedMethods(List.of(HttpMethod.POST.name()));
        configuration.setAllowedHeaders(List.of(HttpHeaders.CONTENT_TYPE));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(Duration.ofMinutes(10));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/leads", configuration);
        return source;
    }
}
