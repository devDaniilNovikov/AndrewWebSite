package ru.andrew.website.observability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
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
                .andExpect(jsonPath("$.status").value("UP"));
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
}
