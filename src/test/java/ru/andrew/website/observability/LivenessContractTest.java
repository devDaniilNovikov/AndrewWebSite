package ru.andrew.website.observability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = NO_DATABASE)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LivenessContractTest {
    @Autowired
    MockMvc mvc;

    @Test
    void livenessIsMinimalAndDependencyFree() throws Exception {
        mvc.perform(get("/actuator/health/liveness").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void readinessAlsoDisablesCachingExactly() throws Exception {
        mvc.perform(get("/actuator/health/readiness").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({
            "HEAD, /actuator/health/liveness",
            "OPTIONS, /actuator/health/liveness",
            "POST, /actuator/health/liveness",
            "HEAD, /actuator/health/readiness",
            "OPTIONS, /actuator/health/readiness",
            "POST, /actuator/health/readiness"
    })
    void healthEndpointsAcceptOnlyGet(String method, String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator", "/actuator/health", "/actuator/metrics", "/actuator/env"})
    void unapprovedActuatorPathsAreNotExternallyAvailable(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/actuator;v=1",
            "/actuator;v=1/health",
            "/actuator;v=1/health/liveness",
            "/actuator%3Bv=1/health",
            "/actu%61tor;v=1/health",
            "/actuator/health/liveness;v=1"
    })
    void actuatorPathParametersCannotBypassThePublicSurface(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isNotFound());
    }

    @Test
    void healthFilterPreservesNonCacheHeadersWhilePinningNoStore() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health/liveness");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new HealthCacheControlFilter().doFilterInternal(request, response, (ignoredRequest, wrapped) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) wrapped;
            httpResponse.addHeader("X-Outcome", "ok");
            httpResponse.addHeader(HttpHeaders.CACHE_CONTROL, "max-age=60");
        });

        org.assertj.core.api.Assertions.assertThat(response.getHeader("X-Outcome")).isEqualTo("ok");
        org.assertj.core.api.Assertions.assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL))
                .isEqualTo("no-store");
    }

    @Test
    void healthFilterRejectsDownstreamCacheControlReplacement() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health/liveness");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new HealthCacheControlFilter().doFilterInternal(request, response, (ignoredRequest, wrapped) ->
                ((HttpServletResponse) wrapped).setHeader(HttpHeaders.CACHE_CONTROL, "max-age=60"));

        org.assertj.core.api.Assertions.assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL))
                .isEqualTo("no-store");
    }

    @Test
    void healthFilterRecognizesLivenessBelowServletContextPath() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/website/actuator/health/liveness");
        request.setContextPath("/website");

        org.assertj.core.api.Assertions.assertThat(
                        new HealthCacheControlFilter().shouldNotFilter(request))
                .isFalse();
    }

    @Test
    void unapprovedActuatorPathStopsTheFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean();

        new HealthCacheControlFilter().doFilterInternal(
                request, response, (ignoredRequest, ignoredResponse) -> chainInvoked.set(true));

        org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(404);
        org.assertj.core.api.Assertions.assertThat(chainInvoked).isFalse();
    }

    @Test
    void nonActuatorPathRemainsOutsideTheFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

        org.assertj.core.api.Assertions.assertThat(
                        new HealthCacheControlFilter().shouldNotFilter(request))
                .isTrue();
    }

    @Test
    void malformedActuatorEncodingFailsClosedWithoutInvokingTheChain() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/actuator%ZZ/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean();
        HealthCacheControlFilter filter = new HealthCacheControlFilter();

        org.assertj.core.api.Assertions.assertThat(filter.shouldNotFilter(request)).isFalse();
        filter.doFilterInternal(
                request, response, (ignoredRequest, ignoredResponse) -> chainInvoked.set(true));

        org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(404);
        org.assertj.core.api.Assertions.assertThat(chainInvoked).isFalse();
    }

    @Test
    void malformedUnrelatedEncodingRemainsOutsideTheHealthFilter() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/%ZZnot-actuator");

        org.assertj.core.api.Assertions.assertThat(
                        new HealthCacheControlFilter().shouldNotFilter(request))
                .isTrue();
    }
}
