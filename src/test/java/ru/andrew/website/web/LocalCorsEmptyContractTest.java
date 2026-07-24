package ru.andrew.website.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.andrew.website.leads.LeadAcceptanceTransaction;

@SpringBootTest(properties = {
        "app.web.rate-limit.enabled=false",
        "app.web.local-cors-origins=",
        "app.leads.fingerprint-key=local-cors-key-material-for-tests-0000001",
        NO_DATABASE
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalCorsEmptyContractTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    LeadAcceptanceTransaction transaction;

    @Test
    void missingLocalOriginsStartsWithAnEmptyAllowlist() throws Exception {
        mvc.perform(options("/api/leads")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
