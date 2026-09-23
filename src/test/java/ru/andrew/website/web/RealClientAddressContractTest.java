package ru.andrew.website.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs Tomcat with the production client-address settings: nginx in the same container
 * connects from loopback and passes the visitor address in {@code X-Real-IP}.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                "server.forward-headers-strategy=native",
                "server.tomcat.remoteip.remote-ip-header=X-Real-IP",
                "server.tomcat.remoteip.internal-proxies=127\\.0\\.0\\.1"
        })
@ActiveProfiles("test")
class RealClientAddressContractTest {
    private static final String HONEYPOT_JSON = "{\"website\":\"bot\"}";

    @LocalServerPort
    int port;

    @Test
    void proxiedVisitorAddressesGetSeparateRateLimitBuckets() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (int index = 0; index < 5; index++) {
                assertThat(submit(client, "198.51.100.10", null).statusCode())
                        .isEqualTo(202);
            }

            assertThat(submit(client, "198.51.100.10", null).statusCode())
                    .isEqualTo(429);
            assertThat(submit(client, "198.51.100.11", null).statusCode())
                    .isEqualTo(202);
        }
    }

    @Test
    void forwardedForHeadersCannotSplitTheProxiedVisitorBucket() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            for (int index = 0; index < 5; index++) {
                assertThat(submit(client, "198.51.100.20", "203.0.113." + index).statusCode())
                        .isEqualTo(202);
            }

            HttpResponse<String> limited = submit(client, "198.51.100.20", "203.0.113.99");

            assertThat(limited.statusCode()).isEqualTo(429);
            assertThat(limited.body()).doesNotContain("198.51.100", "203.0.113");
        }
    }

    private HttpResponse<String> submit(
            HttpClient client, String realIp, String forwardedFor) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/leads"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("X-Real-IP", realIp)
                .POST(HttpRequest.BodyPublishers.ofString(HONEYPOT_JSON));
        if (forwardedFor != null) {
            request.header("X-Forwarded-For", forwardedFor);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
