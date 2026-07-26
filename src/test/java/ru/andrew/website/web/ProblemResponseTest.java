package ru.andrew.website.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ru.andrew.website.leads.LeadMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import tools.jackson.databind.json.JsonMapper;

class ProblemResponseTest {
    private final ProblemResponseWriter writer =
            new ProblemResponseWriter(JsonMapper.builder().build());

    @Test
    void committedResponseIsNeverResetOrRewritten() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpStatus.ACCEPTED.value());
        response.getWriter().write("already committed");
        response.flushBuffer();
        ProblemDetail problem = writer.problem(
                HttpStatus.BAD_REQUEST,
                "urn:andrew:problem:test",
                "Test problem",
                "Must not replace committed output.",
                "/api/leads");

        writer.write(response, problem);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(response.getContentAsString()).isEqualTo("already committed");
    }

    @Test
    void adviceReturnsStableUnsupportedMediaTypeProblemBelowAContextPath() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/website/api/leads");
        request.setContextPath("/website");

        ResponseEntity<ProblemDetail> response =
                new ProblemResponseAdvice(
                        writer, new LeadMetrics(new SimpleMeterRegistry()))
                        .unsupportedMediaType(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody())
                .satisfies(problem -> {
                    assertThat(problem.getType())
                            .hasToString("urn:andrew:problem:unsupported-media-type");
                    assertThat(problem.getTitle()).isEqualTo("Unsupported media type");
                    assertThat(problem.getStatus()).isEqualTo(415);
                    assertThat(problem.getDetail())
                            .isEqualTo("This endpoint accepts application/json only.");
                    assertThat(problem.getInstance()).hasToString("/api/leads");
                });
    }
}
