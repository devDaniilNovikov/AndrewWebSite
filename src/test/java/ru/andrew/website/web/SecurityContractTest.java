package ru.andrew.website.web;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
        "app.web.rate-limit.enabled=false",
        NO_DATABASE
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityContractTest {
    private static final int MAX_REQUEST_BYTES = 16_384;

    @Autowired
    MockMvc mvc;

    @Autowired
    WebProperties properties;

    @Autowired
    ProblemResponseWriter problems;

    @Test
    void publicAllowlistKeepsDiagnosticsAndAuthenticationRoutesClosed() throws Exception {
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
        mvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/metrics")).andExpect(status().isNotFound());
        mvc.perform(get("/api/unknown"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
        mvc.perform(get("/login")).andExpect(status().isNotFound());
        mvc.perform(post("/logout"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
        mvc.perform(post("/unknown").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void staticGetAndHeadRequestsPassThroughTheSecurityBoundary() throws Exception {
        mvc.perform(get("/security-boundary.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("static-boundary\n"));
        mvc.perform(head("/security-boundary.txt"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api;v=1",
            "/api;v=1/unknown",
            "/api%2Funknown",
            "/ap%69/unknown",
            "/api/leads;v=1"
    })
    void encodedAndParameterizedApiPathsCannotEscapeTheClosedNamespace(String path)
            throws Exception {
        mvc.perform(get(path)).andExpect(status().is4xxClientError());
    }

    @Test
    void leadIsStatelessJsonOnlyAndProductionCorsStaysClosed() throws Exception {
        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        expectProblem(mvc.perform(post("/api/leads?ignored=value").content("{}")),
                415,
                "urn:andrew:problem:unsupported-media-type",
                "Unsupported media type",
                "This endpoint accepts application/json only.");
        expectProblem(mvc.perform(post("/api/leads")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{}")),
                415,
                "urn:andrew:problem:unsupported-media-type",
                "Unsupported media type",
                "This endpoint accepts application/json only.");
        expectProblem(mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .content("{}")),
                415,
                "urn:andrew:problem:unsupported-media-type",
                "Unsupported media type",
                "This endpoint accepts application/json only.");

        mvc.perform(post("/api/leads")
                        .header(HttpHeaders.ORIGIN, "https://cross-origin.invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        mvc.perform(options("/api/leads")
                        .header(HttpHeaders.ORIGIN, "https://cross-origin.invalid")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void malformedJsonUsesTheStableInvalidRequestProblem() throws Exception {
        expectProblem(mvc.perform(post("/api/leads?submitted=hidden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json")),
                400,
                "urn:andrew:problem:invalid-request",
                "Invalid request",
                "One or more request fields are invalid.");
    }

    @Test
    void payloadLimitCountsBytesAndPreservesAnExactBoundaryBody() throws Exception {
        String exactAscii = asciiJsonBody(MAX_REQUEST_BYTES);
        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exactAscii))
                .andExpect(status().isAccepted());

        String exactMultibyte = multibyteJsonBody(MAX_REQUEST_BYTES);
        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exactMultibyte))
                .andExpect(status().isAccepted());

        expectProblem(mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asciiJsonBody(MAX_REQUEST_BYTES + 1))),
                413,
                "urn:andrew:problem:payload-too-large",
                "Payload too large",
                "The request body exceeds the allowed size.");
        expectProblem(mvc.perform(post("/api/leads")
                        .header(HttpHeaders.TRANSFER_ENCODING, "chunked")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(multibyteJsonBody(MAX_REQUEST_BYTES + 2))),
                413,
                "urn:andrew:problem:payload-too-large",
                "Payload too large",
                "The request body exceeds the allowed size.");
    }

    @Test
    void streamedOversizedBodyIsRejectedWhenTheLengthIsUnknown() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/leads") {
            @Override
            public int getContentLength() {
                return -1;
            }

            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(asciiJsonBody(MAX_REQUEST_BYTES + 1).getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean();

        new RequestBodyLimitFilter(properties, problems).doFilter(
                request, response, (ignoredRequest, ignoredResponse) -> chainInvoked.set(true));

        assertThat(chainInvoked).isFalse();
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(MediaType.parseMediaType(response.getContentType())
                        .isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .isTrue();
    }

    @Test
    void boundedBodyIsPassedDownstreamWithoutChangingItsBytes() throws Exception {
        byte[] body = "{ \"x\" : \"é\" }".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/leads");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<byte[]> downstreamBody = new AtomicReference<>();

        new RequestBodyLimitFilter(properties, problems).doFilter(
                request,
                response,
                (wrappedRequest, ignoredResponse) -> downstreamBody.set(
                        wrappedRequest.getInputStream().readAllBytes()));

        assertThat(downstreamBody.get()).containsExactly(body);
    }

    private void expectProblem(ResultActions result, int expectedStatus, String type,
            String title, String detail) throws Exception {
        result.andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.type").value(type))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value("/api/leads"));
    }

    private String asciiJsonBody(int byteLength) {
        String body = "{\"x\":\"" + "x".repeat(byteLength - 8) + "\"}";
        if (body.getBytes(StandardCharsets.UTF_8).length != byteLength) {
            throw new IllegalStateException("ASCII fixture length mismatch");
        }
        return body;
    }

    private String multibyteJsonBody(int byteLength) {
        int contentBytes = byteLength - 8;
        if (contentBytes % 2 != 0) {
            throw new IllegalArgumentException("Multibyte fixture requires an even payload length");
        }
        String body = "{\"x\":\"" + "é".repeat(contentBytes / 2) + "\"}";
        if (body.getBytes(StandardCharsets.UTF_8).length != byteLength) {
            throw new IllegalStateException("Multibyte fixture length mismatch");
        }
        return body;
    }
}
