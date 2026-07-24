package ru.andrew.website.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.andrew.website.leads.LeadAcceptanceTransaction;

@SpringBootTest(properties = {
        "app.web.rate-limit.enabled=false",
        "app.web.local-cors-origins[0]=http://127.0.0.1:3000",
        "app.leads.fingerprint-key=local-cors-key-material-for-tests-0000001",
        NO_DATABASE
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalCorsContractTest {
    private static final String ALLOWED_ORIGIN = "http://127.0.0.1:3000";
    private static final String HONEYPOT_JSON = "{\"website\":\"bot\"}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    LeadAcceptanceTransaction transaction;

    @Test
    void configuredLoopbackOriginCanPostWithoutCredentials() throws Exception {
        mvc.perform(options("/api/leads")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));

        mvc.perform(post("/api/leads")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(HONEYPOT_JSON))
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void unconfiguredOriginRemainsDenied() throws Exception {
        mvc.perform(options("/api/leads")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        mvc.perform(post("/api/leads")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
