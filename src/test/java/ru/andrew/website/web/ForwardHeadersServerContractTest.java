package ru.andrew.website.web;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.andrew.website.leads.AcceptanceOutcome;
import ru.andrew.website.leads.LeadAcceptanceTransaction;

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.cloud-platform=kubernetes",
                "LEAD_FINGERPRINT_HMAC_KEY="
                        + "production-forwarded-header-key-material-0001",
                NO_DATABASE
        })
@ActiveProfiles("test")
@Import(ForwardHeadersServerContractTest.ErrorDispatchTestConfiguration.class)
class ForwardHeadersServerContractTest {
    private static final String HONEYPOT_JSON = "{\"website\":\"bot\"}";
    private static final String SENSITIVE_ERROR =
            "sensitive-error-detail-must-not-leave-the-server";

    @LocalServerPort
    int port;

    @TestBean(methodName = "testLeadAcceptanceTransaction", enforceOverride = true)
    LeadAcceptanceTransaction transaction;

    @Test
    void configuredHealthEndpointsRemainAvailableAndMinimal() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> liveness =
                    get(client, "/actuator/health/liveness");
            assertThat(liveness.statusCode()).isEqualTo(200);
            assertThat(liveness.headers().firstValue(HttpHeaders.CACHE_CONTROL))
                    .contains("no-store");
            assertThat(liveness.body()).isEqualTo("{\"status\":\"UP\"}");

            HttpResponse<String> readiness =
                    get(client, "/actuator/health/readiness");
            assertThat(readiness.statusCode()).isEqualTo(503);
            assertThat(readiness.headers().firstValue(HttpHeaders.CACHE_CONTROL))
                    .contains("no-store");
            assertThat(readiness.body()).isEqualTo("{\"status\":\"DOWN\"}");
        }
    }

    @Test
    void directErrorRouteIsClosedForEveryRelevantMethod() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (String method : new String[] {"GET", "HEAD", "POST"}) {
                HttpResponse<String> response =
                        request(client, method, "/error?trace=true&message=true&errors=true");

                assertThat(response.statusCode()).isEqualTo(403);
                assertThat(response.body()).doesNotContain(SENSITIVE_ERROR);
            }
        }
    }

    @Test
    void matrixParameterizedClosedRoutesCannotReachMvcHandlers() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (String path : new String[] {
                    "/error;v=1?trace=true&message=true&errors=true",
                    "/actuator;v=1",
                    "/actuator;v=1/health",
                    "/actuator;v=1/env"
            }) {
                for (String method : new String[] {"GET", "HEAD", "POST"}) {
                    HttpResponse<String> response = request(client, method, path);

                    assertThat(response.statusCode())
                            .as("%s %s", method, path)
                            .isBetween(400, 499);
                    assertThat(response.body()).doesNotContain(SENSITIVE_ERROR);
                }
            }
        }
    }

    @Test
    void genuineServletErrorDispatchRemainsInternalAndRedacted() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = get(
                    client,
                    "/_test/error-dispatch?trace=true&message=true&errors=true");

            assertThat(response.statusCode()).isEqualTo(500);
            assertThat(response.headers().firstValue(HttpHeaders.CONTENT_TYPE))
                    .hasValueSatisfying(value ->
                            assertThat(MediaType.parseMediaType(value)
                                            .isCompatibleWith(MediaType.APPLICATION_JSON))
                                    .isTrue());
            assertThat(response.body())
                    .contains("\"status\":500")
                    .doesNotContain(
                            SENSITIVE_ERROR,
                            "\"exception\"",
                            "\"trace\"",
                            "\"message\"",
                            "\"errors\"",
                            "\"path\"");
        }
    }

    @Test
    void cloudDetectionCannotMakeForwardedHeadersSplitThePhysicalClientBucket()
            throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (int index = 0; index < 5; index++) {
                assertThat(submit(client, "198.51.100." + index).statusCode())
                        .isEqualTo(202);
            }

            HttpResponse<String> limited = submit(client, "203.0.113.9");

            assertThat(limited.statusCode()).isEqualTo(429);
            assertThat(limited.headers().firstValue(HttpHeaders.RETRY_AFTER))
                    .hasValueSatisfying(value ->
                            assertThat(Long.parseLong(value)).isBetween(1L, 60L));
            assertThat(limited.body())
                    .contains("\"type\":\"urn:andrew:problem:rate-limit-exceeded\"")
                    .doesNotContain("198.51.100", "203.0.113");
        }
    }

    private HttpResponse<String> submit(HttpClient client, String forwardedFor)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/leads"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("X-Forwarded-For", forwardedFor)
                .header("Forwarded", "for=" + forwardedFor)
                .POST(HttpRequest.BodyPublishers.ofString(HONEYPOT_JSON))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return request(client, "GET", path);
    }

    private HttpResponse<String> request(HttpClient client, String method, String path)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ErrorDispatchTestConfiguration {
        @Bean
        ErrorDispatchController errorDispatchController() {
            return new ErrorDispatchController();
        }
    }

    private static LeadAcceptanceTransaction testLeadAcceptanceTransaction() {
        return (lead, fingerprint) -> AcceptanceOutcome.CREATED;
    }

    @RestController
    static class ErrorDispatchController {
        @GetMapping("/_test/error-dispatch")
        String fail() {
            throw new IllegalStateException(SENSITIVE_ERROR);
        }
    }
}
