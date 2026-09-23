package ru.andrew.website.leads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.andrew.website.telegram.TelegramDeliveryResult;
import ru.andrew.website.telegram.TelegramGateway;
import ru.andrew.website.telegram.TelegramLeadMessage;

@SpringBootTest(properties = "app.web.rate-limit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class LeadDeliveryContractTest {
    private static final String PHONE = "+7 999 123-45-67";
    private static final String COMMENT = "fictional-private-comment";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TelegramGateway gateway;

    @BeforeEach
    void resetGateway() {
        reset(gateway);
    }

    @Test
    void deliveredLeadIsAcceptedWithTheNormalizedMessage() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(gateway.send(any())).thenReturn(new TelegramDeliveryResult.Delivered());

        submit(requestId, "Иван")
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        ArgumentCaptor<TelegramLeadMessage> sent =
                ArgumentCaptor.forClass(TelegramLeadMessage.class);
        verify(gateway).send(sent.capture());
        assertThat(sent.getValue().requestId()).isEqualTo(requestId);
        assertThat(sent.getValue().name()).isEqualTo("Иван");
        assertThat(sent.getValue().phone()).isEqualTo("79991234567");
        assertThat(sent.getValue().comment()).isEqualTo(COMMENT);
        assertThat(sent.getValue().sourcePath()).isEqualTo("/service/");
        assertThat(sent.getValue().intent()).isEqualTo("repair");
    }

    @Test
    void repeatedSubmissionIsAcceptedWithoutASecondMessage() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(gateway.send(any())).thenReturn(new TelegramDeliveryResult.Delivered());

        submit(requestId, "Иван").andExpect(status().isAccepted());
        submit(requestId, "Иван").andExpect(status().isAccepted());

        verify(gateway, times(1)).send(any());
    }

    @Test
    void reusedRequestIdWithAnotherPayloadIsAConflict() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(gateway.send(any())).thenReturn(new TelegramDeliveryResult.Delivered());
        submit(requestId, "Иван").andExpect(status().isAccepted());

        submit(requestId, "Пётр")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:andrew:problem:idempotency-conflict"));

        verify(gateway, times(1)).send(any());
    }

    @ParameterizedTest
    @MethodSource("failedDeliveries")
    void failedDeliveryIsAnHonestGenericProblemWithoutPii(
            TelegramDeliveryResult failure, String code, CapturedOutput output)
            throws Exception {
        when(gateway.send(any())).thenReturn(failure);

        submit(UUID.randomUUID(), "Иван")
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.type").value("urn:andrew:problem:service-unavailable"))
                .andExpect(jsonPath("$.title").value("Service unavailable"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail")
                        .value("The request could not be delivered right now."))
                .andExpect(jsonPath("$.instance").value("/api/leads"));

        assertThat(output.getAll())
                .contains("Telegram delivery failed: " + code)
                .doesNotContain("Иван", PHONE, "79991234567", COMMENT);
    }

    @Test
    void failedDeliveryIsNotRememberedSoTheRetryIsSent() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(gateway.send(any()))
                .thenReturn(new TelegramDeliveryResult.Retryable("network", null))
                .thenReturn(new TelegramDeliveryResult.Delivered());

        submit(requestId, "Иван").andExpect(status().isServiceUnavailable());
        submit(requestId, "Иван").andExpect(status().isAccepted());

        verify(gateway, times(2)).send(any());
    }

    @Test
    void honeypotNeverReachesTelegram() throws Exception {
        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"website\":\"filled-by-bot\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verifyNoInteractions(gateway);
    }

    private static Stream<Arguments> failedDeliveries() {
        return Stream.of(
                Arguments.of(
                        new TelegramDeliveryResult.Retryable("telegram_5xx", null),
                        "telegram_5xx"),
                Arguments.of(
                        new TelegramDeliveryResult.PermanentFailure("telegram_permanent_403"),
                        "telegram_permanent_403"));
    }

    private ResultActions submit(UUID requestId, String name) throws Exception {
        return mvc.perform(post("/api/leads?submitted=private")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"requestId":"%s","name":"%s","phone":"%s",
                         "comment":"%s","sourcePath":"/service/","intent":"repair",
                         "consent":true,"website":""}
                        """.formatted(requestId, name, PHONE, COMMENT)));
    }
}
