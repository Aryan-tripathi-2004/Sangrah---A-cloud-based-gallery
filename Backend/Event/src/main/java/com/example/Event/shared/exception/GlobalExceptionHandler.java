package com.example.Event.shared.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        log.warn("Validation failed with {} error(s)", ex.getBindingResult().getErrorCount());

        var violations = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldViolation(
                        error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName(),
                        error.getDefaultMessage()))
                .toList();

        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more request fields are invalid.",
                request);
        problem.setProperty("violations", violations);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {
        log.warn("Constraint validation failed: {}", ex.getMessage());

        var violations = ex.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();

        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more request fields are invalid.",
                request);
        problem.setProperty("violations", violations);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> handleMissingRequestHeader(
            MissingRequestHeaderException ex,
            WebRequest request) {
        log.warn("Missing request header: {}", ex.getHeaderName());
        HttpStatus status = "X-User-Id".equalsIgnoreCase(ex.getHeaderName())
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;
        String title = status == HttpStatus.UNAUTHORIZED ? "Authentication required" : "Missing request header";
        String detail = status == HttpStatus.UNAUTHORIZED
                ? "Required authentication header is missing."
                : "Required request header is missing: " + ex.getHeaderName();
        return problemResponse(status, title, detail, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            WebRequest request) {
        log.warn("Unreadable request body: {}", ex.getMessage());
        return problemResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                "Request body contains an invalid or unsupported value.",
                request);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(
            DomainValidationException ex,
            WebRequest request) {
        log.warn("Domain validation failed: {}", ex.getMessage());
        return problemResponse(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return problemResponse(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationRequired(
            AuthenticationRequiredException ex,
            WebRequest request) {
        log.warn("Authentication required: {}", ex.getMessage());
        return problemResponse(HttpStatus.UNAUTHORIZED, "Authentication required", ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ProblemDetail> handleForbiddenOperation(
            ForbiddenOperationException ex,
            WebRequest request) {
        log.warn("Forbidden operation: {}", ex.getMessage());
        return problemResponse(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ProblemDetail> handleSecurityException(
            SecurityException ex,
            WebRequest request) {
        log.warn("Security exception: {}", ex.getMessage());
        return problemResponse(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return problemResponse(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage(), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLockingFailure(
            OptimisticLockingFailureException ex,
            WebRequest request) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        return problemResponse(
                HttpStatus.CONFLICT,
                "Concurrent modification conflict",
                "The resource was modified by another request. Please reload and try again.",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(
            Exception ex,
            WebRequest request) {
        log.error("Unexpected error occurred", ex);
        return problemResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred.",
                request);
    }

    private ResponseEntity<ProblemDetail> problemResponse(
            HttpStatus status,
            String title,
            String detail,
            WebRequest request) {
        return ResponseEntity.status(status).body(problem(status, title, detail, request));
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail,
            WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getDescription(false).replace("uri=", ""));
        return problem;
    }

    public record FieldViolation(String field, String message) {
    }
}
