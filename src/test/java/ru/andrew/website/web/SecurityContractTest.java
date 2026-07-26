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

import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.andrew.website.leads.LeadAcceptanceTransaction;
import ru.andrew.website.leads.LeadMetrics;

@SpringBootTest(properties = {
        "app.web.rate-limit.enabled=false",
        NO_DATABASE
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityContractTest {
    private static final int MAX_REQUEST_BYTES = 16_384;
    private static final String HONEYPOT_JSON = "{\"website\":\"bot\"}";

    @Autowired
    MockMvc mvc;

    @Autowired
    WebProperties properties;

    @Autowired
    ProblemResponseWriter problems;

    @Autowired
    FilterChainProxy springSecurityFilterChain;

    @MockitoBean
    LeadAcceptanceTransaction transaction;

    @ParameterizedTest
    @ValueSource(strings = {"/api", "/api/unknown", "/actuator", "/actuator/health"})
    void securityChainDeniesClosedNamespacesWithoutOuterServletFilters(String path)
            throws Exception {
        MockHttpServletRequest request = directRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstreamInvoked = new AtomicBoolean();

        springSecurityFilterChain.doFilter(
                request,
                response,
                (ignoredRequest, ignoredResponse) -> downstreamInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(downstreamInvoked).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/apiary", "/actuatorish"})
    void securityChainDoesNotTreatNearCollisionsAsClosedNamespaces(String path)
            throws Exception {
        MockHttpServletRequest request = directRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstreamInvoked = new AtomicBoolean();

        springSecurityFilterChain.doFilter(
                request,
                response,
                (ignoredRequest, ignoredResponse) -> downstreamInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(downstreamInvoked).isTrue();
    }

    @Test
    void securityChainDeniesNonLocalPreflightWithoutOuterServletFilters()
            throws Exception {
        MockHttpServletRequest request = directRequest("OPTIONS", "/api/leads");
        request.addHeader(HttpHeaders.ORIGIN, "https://cross-origin.invalid");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean downstreamInvoked = new AtomicBoolean();

        springSecurityFilterChain.doFilter(
                request,
                response,
                (ignoredRequest, ignoredResponse) -> downstreamInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        assertThat(downstreamInvoked).isFalse();
    }

    @Test
    void securityChainAllowsOnlyErrorDispatchToTheExactErrorPath() throws Exception {
        MockHttpServletRequest directRequest = directRequest("GET", "/error");
        MockHttpServletResponse directResponse = new MockHttpServletResponse();
        AtomicBoolean directDownstream = new AtomicBoolean();

        springSecurityFilterChain.doFilter(
                directRequest,
                directResponse,
                (ignoredRequest, ignoredResponse) -> directDownstream.set(true));

        assertThat(directResponse.getStatus()).isEqualTo(403);
        assertThat(directDownstream).isFalse();

        MockHttpServletRequest errorRequest = directRequest("GET", "/error");
        errorRequest.setDispatcherType(DispatcherType.ERROR);
        MockHttpServletResponse errorResponse = new MockHttpServletResponse();
        AtomicBoolean errorDownstream = new AtomicBoolean();

        springSecurityFilterChain.doFilter(
                errorRequest,
                errorResponse,
                (ignoredRequest, ignoredResponse) -> errorDownstream.set(true));

        assertThat(errorResponse.getStatus()).isEqualTo(200);
        assertThat(errorDownstream).isTrue();
    }

    @Test
    void securityChainUsesApplicationRelativePathsWithAServletContext() throws Exception {
        MockHttpServletRequest leadRequest =
                directRequest("POST", "/website/api/leads", "/website", "/api/leads");
        leadRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);
        MockHttpServletResponse leadResponse = new MockHttpServletResponse();
        AtomicBoolean leadDownstream = new AtomicBoolean();

        springSecurityFilterChain.doFilter(
                leadRequest,
                leadResponse,
                (ignoredRequest, ignoredResponse) -> leadDownstream.set(true));

        assertThat(leadResponse.getStatus()).isEqualTo(200);
        assertThat(leadDownstream).isTrue();

        MockHttpServletRequest closedRequest =
                directRequest("GET", "/website/api/unknown", "/website", "/api/unknown");
        MockHttpServletResponse closedResponse = new MockHttpServletResponse();
        AtomicBoolean closedDownstream = new AtomicBoolean();

        springSecurityFilterChain.doFilter(
                closedRequest,
                closedResponse,
                (ignoredRequest, ignoredResponse) -> closedDownstream.set(true));

        assertThat(closedResponse.getStatus()).isEqualTo(403);
        assertThat(closedDownstream).isFalse();
    }

    @Test
    void publicAllowlistKeepsDiagnosticsAndAuthenticationRoutesClosed() throws Exception {
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
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
            "/api/leads;v=1",
            "/api/leads/",
            "/api/leads/descendant"
    })
    void encodedAndParameterizedApiPathsCannotEscapeTheClosedNamespace(String path)
            throws Exception {
        mvc.perform(get(path)).andExpect(status().is4xxClientError());
    }

    @Test
    void matrixParameterizedLeadPostIsNotAnExactAllowlistMatch() throws Exception {
        mvc.perform(post("/api/leads;v=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(HONEYPOT_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void leadIsStatelessJsonOnlyAndProductionCorsStaysClosed() throws Exception {
        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(HONEYPOT_JSON))
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
                        .content(HONEYPOT_JSON))
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

        new RequestBodyLimitFilter(
                properties,
                problems,
                new LeadMetrics(new SimpleMeterRegistry())).doFilter(
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

        new RequestBodyLimitFilter(
                properties,
                problems,
                new LeadMetrics(new SimpleMeterRegistry())).doFilter(
                request,
                response,
                (wrappedRequest, ignoredResponse) -> downstreamBody.set(
                        wrappedRequest.getInputStream().readAllBytes()));

        assertThat(downstreamBody.get()).containsExactly(body);
    }

    private static MockHttpServletRequest directRequest(String method, String path) {
        return directRequest(method, path, "", path);
    }

    private static MockHttpServletRequest directRequest(
            String method, String requestUri, String contextPath, String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
        request.setContextPath(contextPath);
        request.setServletPath(servletPath);
        return request;
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
        String body = "{\"website\":\"" + "x".repeat(byteLength - 14) + "\"}";
        if (body.getBytes(StandardCharsets.UTF_8).length != byteLength) {
            throw new IllegalStateException("ASCII fixture length mismatch");
        }
        return body;
    }

    private String multibyteJsonBody(int byteLength) {
        int contentBytes = byteLength - 14;
        if (contentBytes % 2 != 0) {
            throw new IllegalArgumentException("Multibyte fixture requires an even payload length");
        }
        String body = "{\"website\":\"" + "é".repeat(contentBytes / 2) + "\"}";
        if (body.getBytes(StandardCharsets.UTF_8).length != byteLength) {
            throw new IllegalStateException("Multibyte fixture length mismatch");
        }
        return body;
    }
}
