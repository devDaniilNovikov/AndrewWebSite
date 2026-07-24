package ru.andrew.website.leads;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.andrew.website.testing.TestAutoConfigurationExclusions.NO_DATABASE;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;

@SpringBootTest(properties = {
        "app.web.rate-limit.enabled=false",
        NO_DATABASE
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class LeadUnavailableContractTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    LeadAcceptanceTransaction transaction;

    @BeforeEach
    void resetTransaction() {
        reset(transaction);
    }

    @ParameterizedTest
    @MethodSource("durabilityFailures")
    void durabilityFailuresUseTheExactGenericProblemWithoutCauseOrPii(
            String phase, RuntimeException failure, CapturedOutput output) throws Exception {
        when(transaction.accept(any(), any())).thenThrow(failure);

        mvc.perform(post("/api/leads?submitted=private")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.type").value("urn:andrew:problem:service-unavailable"))
                .andExpect(jsonPath("$.title").value("Service unavailable"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail")
                        .value("The request cannot be accepted durably at this time."))
                .andExpect(jsonPath("$.instance").value("/api/leads"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("fictional-database-detail"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("fictional-private-comment"))));

        assertNoPrivateData(output);
    }

    @Test
    void honeypotNeverTouchesTheUnavailableTransaction() throws Exception {
        when(transaction.accept(any(), any())).thenThrow(
                new DataAccessResourceFailureException("fictional-database-detail"));

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"website\":\"filled-by-bot\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verifyNoInteractions(transaction);
    }

    private static Stream<Arguments> durabilityFailures() {
        return Stream.of(
                Arguments.of(
                        "data access",
                        new DataAccessResourceFailureException("fictional-database-detail")),
                Arguments.of(
                        "transaction begin",
                        new CannotCreateTransactionException("fictional-begin-detail")),
                Arguments.of(
                        "transaction commit",
                        new TransactionSystemException("fictional-commit-detail")));
    }

    private static String validBody() {
        return """
                {"requestId":"77777777-7777-4777-8777-777777777777","name":"Иван",
                 "phone":"+7 999 123-45-67","comment":"fictional-private-comment",
                 "sourcePath":"/service/","intent":"repair","consent":true,"website":""}
                """;
    }

    private static void assertNoPrivateData(CapturedOutput output) {
        org.assertj.core.api.Assertions.assertThat(output.getAll())
                .doesNotContain(
                        "fictional-database-detail",
                        "fictional-begin-detail",
                        "fictional-commit-detail",
                        "fictional-private-comment",
                        "+7 999 123-45-67");
    }
}
