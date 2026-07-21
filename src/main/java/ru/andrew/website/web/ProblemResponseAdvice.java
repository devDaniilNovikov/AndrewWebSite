package ru.andrew.website.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
final class ProblemResponseAdvice {
    private final ProblemResponseWriter problems;

    ProblemResponseAdvice(ProblemResponseWriter problems) {
        this.problems = problems;
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class
    })
    ResponseEntity<ProblemDetail> invalidRequest(HttpServletRequest request) {
        ProblemDetail problem = problems.problem(
                HttpStatus.BAD_REQUEST,
                "urn:andrew:problem:invalid-request",
                "Invalid request",
                "One or more request fields are invalid.",
                path(request));
        return response(problem);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> unsupportedMediaType(HttpServletRequest request) {
        ProblemDetail problem = problems.problem(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "urn:andrew:problem:unsupported-media-type",
                "Unsupported media type",
                "This endpoint accepts application/json only.",
                path(request));
        return response(problem);
    }

    private ResponseEntity<ProblemDetail> response(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
