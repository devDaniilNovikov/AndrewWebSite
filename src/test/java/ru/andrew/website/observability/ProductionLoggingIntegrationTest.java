package ru.andrew.website.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.andrew.website.telegram.TelegramDeliveryResult;
import ru.andrew.website.telegram.TelegramGateway;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
        "LEAD_FINGERPRINT_HMAC_KEY="
                + "fictional-production-fingerprint-key-material-0001",
        "TELEGRAM_BOT_TOKEN=fictional-telegram-token",
        "TELEGRAM_CHAT_ID=fictional-telegram-chat",
        "spring.application.group=fictional-private-group"
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@ExtendWith(OutputCaptureExtension.class)
class ProductionLoggingIntegrationTest {
    private static final String FAILED_REQUEST_ID =
            "77777777-7777-4777-8777-777777777777";
    private static final String DELIVERED_REQUEST_ID =
            "88888888-8888-4888-8888-888888888888";
    private static final String PHONE = "+7 999 123-45-67";
    private static final String COMMENT =
            "fictional-private-comment";
    private static final List<String> PRIVATE_DATA = List.of(
            "Иван",
            PHONE,
            "79991234567",
            COMMENT,
            FAILED_REQUEST_ID,
            DELIVERED_REQUEST_ID,
            "fictional-production-fingerprint-key-material-0001",
            "fictional-telegram-token",
            "fictional-telegram-chat",
            "fictional-private-group");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TelegramGateway gateway;

    @Test
    void failedDeliveryWritesOneEcsErrorWithoutPrivateData(
            CapturedOutput output) throws Exception {
        when(gateway.send(any()))
                .thenReturn(new TelegramDeliveryResult.Retryable("telegram_5xx", null));

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(FAILED_REQUEST_ID)))
                .andExpect(status().isServiceUnavailable());

        assertThat(jsonEvents(output))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.path("log").path("level").asText())
                            .isEqualTo("ERROR");
                    assertThat(event.path("log").path("logger").asText())
                            .isEqualTo("ru.andrew.website.leads.TelegramLeadDelivery");
                    assertThat(event.path("message").asText())
                            .isEqualTo("Telegram delivery failed: telegram_5xx");
                    assertThat(event.path("ecs").has("version")).isTrue();
                    assertThat(event.has("error")).isFalse();
                });
        assertThat(output.getAll()).doesNotContain(PRIVATE_DATA);
    }

    @Test
    void deliveredLeadAndPublicProbesLogNothing(CapturedOutput output)
            throws Exception {
        when(gateway.send(any())).thenReturn(new TelegramDeliveryResult.Delivered());

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(DELIVERED_REQUEST_ID)))
                .andExpect(status().isAccepted());
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isNotFound());

        assertThat(jsonEvents(output)).isEmpty();
        assertThat(output.getAll()).doesNotContain(PRIVATE_DATA);
    }

    static JsonNode parseJson(String line) {
        try {
            return JsonMapper.builder().build().readTree(line);
        } catch (tools.jackson.core.JacksonException invalidJson) {
            throw new AssertionError(invalidJson);
        }
    }

    private static List<JsonNode> jsonEvents(CapturedOutput output) {
        return output.getAll().lines()
                .filter(line -> line.startsWith("{"))
                .map(ProductionLoggingIntegrationTest::parseJson)
                .toList();
    }

    private static String validBody(String requestId) {
        return """
                {"requestId":"%s","name":"Иван","phone":"%s",
                 "comment":"%s","sourcePath":"/service/","intent":"repair",
                 "consent":true,"website":""}
                """.formatted(requestId, PHONE, COMMENT);
    }
}
