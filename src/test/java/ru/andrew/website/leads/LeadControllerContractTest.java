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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.web.rate-limit.enabled=false",
        NO_DATABASE
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeadControllerContractTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    LeadAcceptanceTransaction transaction;

    @BeforeEach
    void defaultAcceptance() {
        reset(transaction);
        when(transaction.accept(any(), any())).thenReturn(AcceptanceOutcome.CREATED);
    }

    @Test
    void legitimateAndHoneypotRequestsReturnTheSameEmptyAcceptance() throws Exception {
        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("11111111-1111-4111-8111-111111111111", "Комментарий", "")))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        reset(transaction);
        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"website\":\"filled-by-bot\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
        verifyNoInteractions(transaction);

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"website\":\"filled-by-bot\",\"comment\":null}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
        verifyNoInteractions(transaction);
    }

    @ParameterizedTest
    @MethodSource("legitimateWebsiteVariants")
    void legitimateWebsiteMayBeMissingNullOrEmpty(String websiteProperty) throws Exception {
        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBodyWithWebsiteProperty(websiteProperty)))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    @Test
    void normalizedUnicodeCodePointBoundariesAreAccepted() throws Exception {
        String body = """
                {"requestId":"12121212-1212-4212-8212-121212121212",
                 "name":" %s ","phone":"%s","comment":"%s","sourcePath":"%s",
                 "intent":"maintenance","consent":true,"website":""}
                """.formatted(
                "😀".repeat(100),
                "1234567" + " ".repeat(30),
                "😀".repeat(1_000),
                "/" + "😀".repeat(2_047));

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    @ParameterizedTest
    @MethodSource("acceptedOutcomes")
    void internalAcceptanceOutcomeIsNeverDisclosed(AcceptanceOutcome outcome) throws Exception {
        when(transaction.accept(any(), any())).thenReturn(outcome);

        mvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("22222222-2222-4222-8222-222222222222", null, "")))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void invalidLegitimateShapesUseTheExactGenericProblem(String body) throws Exception {
        expectProblem(body,
                "urn:andrew:problem:invalid-request",
                "Invalid request",
                400,
                "One or more request fields are invalid.");
    }

    @ParameterizedTest
    @MethodSource("wrongTypedHoneypotFields")
    void honeypotStillRejectsWrongKnownJsonTypes(String body) throws Exception {
        expectProblem(body,
                "urn:andrew:problem:invalid-request",
                "Invalid request",
                400,
                "One or more request fields are invalid.");
        verifyNoInteractions(transaction);
    }

    @Test
    void idempotencyConflictUsesTheExactProblemWithoutSubmittedValues() throws Exception {
        when(transaction.accept(any(), any())).thenThrow(new IdempotencyConflictException());

        expectProblem(validBody(
                        "33333333-3333-4333-8333-333333333333",
                        "fictional-private-comment",
                        ""),
                "urn:andrew:problem:idempotency-conflict",
                "Idempotency conflict",
                409,
                "The request identifier cannot be reused for this payload.");
    }

    @Test
    void trailingJsonValueIsRejectedAsMalformedInput() throws Exception {
        expectProblem(
                validBody(
                                "15151515-1515-4515-8515-151515151515",
                                null,
                                "")
                        + "{}",
                "urn:andrew:problem:invalid-request",
                "Invalid request",
                400,
                "One or more request fields are invalid.");
    }

    private void expectProblem(
            String body, String type, String title, int expectedStatus, String detail)
            throws Exception {
        mvc.perform(post("/api/leads?submitted=private")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.type").value(type))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value("/api/leads"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("fictional-private-comment"))));
    }

    private static Stream<AcceptanceOutcome> acceptedOutcomes() {
        return Stream.of(
                AcceptanceOutcome.CREATED,
                AcceptanceOutcome.DUPLICATE,
                AcceptanceOutcome.RETAINED);
    }

    private static Stream<String> invalidBodies() {
        return Stream.of(
                "null",
                "{}",
                "{\"website\":\"\"}",
                validBody("not-a-uuid", null, ""),
                """
                {"requestId":"11111111-1111-4111-8111-111111111111","name":" И ",
                 "phone":"+7 999 123-45-67","sourcePath":"/service/",
                 "intent":"repair","consent":true,"website":""}
                """,
                """
                {"requestId":"11111111-1111-4111-8111-111111111111","name":"Иван",
                 "phone":"123456","sourcePath":"/service/",
                 "intent":"repair","consent":true,"website":""}
                """,
                """
                {"requestId":"11111111-1111-4111-8111-111111111111","name":"Иван",
                 "phone":"+7 999 123-45-67","sourcePath":"/service/../admin",
                 "intent":"repair","consent":true,"website":""}
                """,
                """
                {"requestId":"11111111-1111-4111-8111-111111111111","name":"Иван",
                 "phone":"+7 999 123-45-67","sourcePath":"/service/",
                 "intent":"repair","consent":false,"website":""}
                """,
                """
                {"requestId":"11111111-1111-4111-8111-111111111111","name":"Иван",
                 "phone":"+7 999 123-45-67","sourcePath":"/service/",
                 "intent":"repair","consent":true,"website":"","unexpected":"rejected"}
                """,
                validBodyWithName("😀".repeat(101)),
                validBodyWithPhone("1 2 3 4 5 6 7" + " ".repeat(19) + "8"),
                validBodyWithPhone("123456a"),
                validBodyWithComment("😀".repeat(1_001)),
                validBodyWithSourcePath("/" + "😀".repeat(2_048)));
    }

    private static Stream<Arguments> wrongTypedHoneypotFields() {
        return Stream.of(
                Arguments.of("{\"website\":\"bot\",\"name\":123}"),
                Arguments.of("{\"website\":\"bot\",\"phone\":false}"),
                Arguments.of("{\"website\":\"bot\",\"comment\":123}"),
                Arguments.of("{\"website\":\"bot\",\"sourcePath\":123}"),
                Arguments.of("{\"website\":\"bot\",\"intent\":0}"),
                Arguments.of("{\"website\":\"bot\",\"consent\":\"true\"}"),
                Arguments.of("{\"website\":\"bot\",\"requestId\":123}"),
                Arguments.of("{\"website\":\"bot\",\"requestId\":null}"),
                Arguments.of("{\"website\":\"bot\",\"name\":null}"),
                Arguments.of("{\"website\":\"bot\",\"phone\":null}"),
                Arguments.of("{\"website\":\"bot\",\"sourcePath\":null}"),
                Arguments.of("{\"website\":\"bot\",\"intent\":null}"),
                Arguments.of("{\"website\":\"bot\",\"consent\":null}"),
                Arguments.of("{\"website\":123}"),
                Arguments.of("{\"website\":\"bot\",\"unexpected\":\"rejected\"}"));
    }

    private static Stream<String> legitimateWebsiteVariants() {
        return Stream.of("", ",\"website\":null", ",\"website\":\"\"");
    }

    private static String validBody(String requestId, String comment, String website) {
        String commentProperty = comment == null
                ? ""
                : ",\"comment\":\"" + comment + "\"";
        return """
                {"requestId":"%s","name":"Иван","phone":"+7 999 123-45-67"%s,
                 "sourcePath":"/service/","intent":"repair","consent":true,"website":"%s"}
                """.formatted(requestId, commentProperty, website);
    }

    private static String validBodyWithWebsiteProperty(String websiteProperty) {
        return """
                {"requestId":"13131313-1313-4313-8313-131313131313","name":"Иван",
                 "phone":"+7 999 123-45-67","sourcePath":"/service/",
                 "intent":"repair","consent":true%s}
                """.formatted(websiteProperty);
    }

    private static String validBodyWithName(String name) {
        return validBodyWithField("\"name\":\"Иван\"", "\"name\":\"" + name + "\"");
    }

    private static String validBodyWithPhone(String phone) {
        return validBodyWithField(
                "\"phone\":\"+7 999 123-45-67\"", "\"phone\":\"" + phone + "\"");
    }

    private static String validBodyWithComment(String comment) {
        return validBodyWithField(
                "\"website\":\"\"", "\"comment\":\"" + comment + "\",\"website\":\"\"");
    }

    private static String validBodyWithSourcePath(String sourcePath) {
        return validBodyWithField(
                "\"sourcePath\":\"/service/\"", "\"sourcePath\":\"" + sourcePath + "\"");
    }

    private static String validBodyWithField(String original, String replacement) {
        return validBody(
                        "14141414-1414-4414-8414-141414141414",
                        null,
                        "")
                .replace(original, replacement);
    }
}
