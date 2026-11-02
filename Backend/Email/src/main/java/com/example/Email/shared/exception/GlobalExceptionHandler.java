package com.example.Email.shared.exception;

import jakarta.mail.MessagingException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String BASE_ERROR_URI = "https://api.sangrah.com/errors/";

    public record FieldViolation(String field, String message) {}

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        log.warn("Validation failed with {} error(s)", ex.getBindingResult().getErrorCount());

        List<FieldViolation> violations = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldViolation(
                        error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName(),
                        error.getDefaultMessage()))
                .toList();

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "One or more request fields failed validation.",
                "validation-failed",
                request);
        problem.setProperty("errors", errors);
        problem.setProperty("violations", violations);
        problem.setProperty("service", "email-service");

        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        log.warn("Malformed or unreadable request body: {}", ex.getMessage());
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Malformed Request Body",
                "The request payload is malformed or contains unsupported values.",
                "invalid-body",
                request);
        problem.setProperty("service", "email-service");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {

        log.warn("Constraint violation: {}", ex.getMessage());
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v ->
                errors.put(v.getPropertyPath().toString(), v.getMessage()));

        ProblemDetail problem = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Constraint Violation",
                "One or more request parameters failed validation.",
                "constraint-violation",
                request);
        problem.setProperty("errors", errors);
        problem.setProperty("service", "email-service");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ProblemDetail> handleMessagingException(
            MessagingException ex,
            WebRequest request) {
        log.error("SMTP MessagingException: {}", ex.getMessage(), ex);
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.BAD_GATEWAY,
                "SMTP Delivery Error",
                "The email could not be dispatched due to an SMTP transport error.",
                "smtp-error",
                request);
        problem.setProperty("service", "email-service");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest request) {
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                "not-found",
                request);
        problem.setProperty("service", "email-service");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(EmailProcessingException.class)
    public ResponseEntity<ProblemDetail> handleEmailProcessing(
            EmailProcessingException ex,
            WebRequest request) {
        log.error("Email processing failed: {}", ex.getMessage(), ex);
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Email Processing Error",
                "Email processing failed. Please try again later.",
                "email-processing-error",
                request);
        problem.setProperty("service", "email-service");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request) {
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.BAD_REQUEST, "Invalid Argument", ex.getMessage(), "bad-request", request);
        problem.setProperty("service", "email-service");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(
            Exception ex,
            WebRequest request) {
        log.error("Unhandled internal exception in Email service", ex);
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected internal server error occurred.",
                "internal-server-error",
                request);
        problem.setProperty("service", "email-service");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ProblemDetail buildProblemDetail(
            HttpStatus status,
            String title,
            String detail,
            String slug,
            WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(BASE_ERROR_URI + slug));
        problem.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
