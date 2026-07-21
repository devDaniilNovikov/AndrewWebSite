package ru.andrew.website.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.web.rate-limit.enabled=false",
        "app.web.local-cors-origins=",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalCorsEmptyContractTest {
    @Autowired
    MockMvc mvc;

    @Test
    void missingLocalOriginsStartsWithAnEmptyAllowlist() throws Exception {
        mvc.perform(options("/api/leads")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
