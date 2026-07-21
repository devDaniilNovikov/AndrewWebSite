package ru.andrew.website.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitFilterContractTest {
    private static final int MAX_REQUEST_BYTES = 16_384;

    @Autowired
    MockMvc mvc;

    @Test
    void oversizedBodyDoesNotConsumeAdmissionAndForwardedHeadersNeverSplitTheBucket()
            throws Exception {
        RequestPostProcessor connection = request -> {
            request.setRemoteAddr("192.0.2.25");
            return request;
        };

        mvc.perform(post("/api/leads")
                        .with(connection)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asciiJsonBody(MAX_REQUEST_BYTES + 1)))
                .andExpect(status().isContentTooLarge());

        for (int index = 0; index < 5; index++) {
            mvc.perform(post("/api/leads")
                            .with(connection)
                            .header("X-Forwarded-For", "198.51.100." + index)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isAccepted());
        }

        mvc.perform(post("/api/leads?ignored=value")
                        .with(connection)
                        .header("Forwarded", "for=203.0.113.9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"))
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.type").value("urn:andrew:problem:rate-limit-exceeded"))
                .andExpect(jsonPath("$.title").value("Too many requests"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.detail").value("Wait before submitting another request."))
                .andExpect(jsonPath("$.instance").value("/api/leads"));
    }

    private String asciiJsonBody(int byteLength) {
        String body = "{\"x\":\"" + "x".repeat(byteLength - 8) + "\"}";
        if (body.getBytes(StandardCharsets.UTF_8).length != byteLength) {
            throw new IllegalStateException("ASCII fixture length mismatch");
        }
        return body;
    }
}
