package com.example.Media.shared.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;

/**
 * Centralised exception handler for all controllers in the Media microservice.
 *
 * <p>All responses conform to RFC-9457 (Problem Details for HTTP APIs) via Spring's
 * native {@link ProblemDetail} support, providing a consistent, machine-readable
 * error envelope across every failure mode.</p>
 *
 * <p>Handler precedence (most-specific to least-specific):
 * <ol>
 *   <li>{@link MethodArgumentNotValidException} — {@code @Valid} binding failures → 400</li>
 *   <li>{@link ConstraintViolationException} — method-level constraint violations → 400</li>
 *   <li>{@link HttpMessageNotReadableException} — malformed JSON body → 400</li>
 *   <li>{@link MethodArgumentTypeMismatchException} — invalid {@code @RequestParam} enum value → 400</li>
 *   <li>{@link IllegalArgumentException} — explicit domain guard assertions → 400</li>
 *   <li>{@link MissingRequestHeaderException} — {@code X-User-Id} absent → 401, others → 400</li>
 *   <li>{@link ResourceNotFoundException} — media asset not found / soft-deleted → 404</li>
 *   <li>{@link OptimisticLockingFailureException} — concurrent write conflict → 409</li>
 *   <li>{@link Exception} — all unhandled throwables → 500 (message suppressed)</li>
 * </ol>
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // 400 Bad Request
    // -------------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
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
                .map(v -> new FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();

        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more request fields are invalid.",
                request);
        problem.setProperty("violations", violations);
        return ResponseEntity.badRequest().body(problem);
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

    /**
     * Handles cases where a {@code @RequestParam} or {@code @PathVariable} cannot be
     * converted to its target type — most commonly an invalid {@link com.example.Media.shared.enums.MediaDomain}
     * enum value (e.g., {@code domain=INVALID_DOMAIN}).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {

        String detail = String.format(
                "Invalid value '%s' for parameter '%s'. %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null && ex.getRequiredType().isEnum()
                        ? "Accepted values: " + java.util.Arrays.toString(ex.getRequiredType().getEnumConstants())
                        : "Please provide a valid value.");

        log.warn("Type mismatch: {}", detail);
        return problemResponse(HttpStatus.BAD_REQUEST, "Invalid parameter value", detail, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());
        return problemResponse(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage(), request);
    }

    // -------------------------------------------------------------------------
    // 401 Unauthorized / 400 Bad Request — missing headers
    // -------------------------------------------------------------------------

    /**
     * Handles a missing {@code X-User-Id} header (populated by the API Gateway after JWT
     * validation) as {@code 401 Unauthorized}. All other missing headers return {@code 400}.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> handleMissingRequestHeader(
            MissingRequestHeaderException ex,
            WebRequest request) {

        log.warn("Missing request header: {}", ex.getHeaderName());

        boolean isAuthHeader = "X-User-Id".equalsIgnoreCase(ex.getHeaderName());
        HttpStatus status = isAuthHeader ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        String title  = isAuthHeader ? "Authentication required"    : "Missing request header";
        String detail = isAuthHeader
                ? "The X-User-Id authentication header is required but was not provided."
                : "Required request header '" + ex.getHeaderName() + "' is missing.";

        return problemResponse(status, title, detail, request);
    }

    // -------------------------------------------------------------------------
    // 404 Not Found
    // -------------------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());
        return problemResponse(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage(), request);
    }

    // -------------------------------------------------------------------------
    // 409 Conflict
    // -------------------------------------------------------------------------

    /**
     * Handles optimistic locking conflicts arising from the {@code @Version} field
     * on {@code MediaDocument} and {@code StorageUsageLedgerDocument}.
     * The client must reload the resource and retry.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLocking(
            OptimisticLockingFailureException ex,
            WebRequest request) {

        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        return problemResponse(
                HttpStatus.CONFLICT,
                "Concurrent modification conflict",
                "The resource was modified by another request. Please reload and try again.",
                request);
    }

    // -------------------------------------------------------------------------
    // 500 Internal Server Error — catch-all
    // -------------------------------------------------------------------------

    /**
     * Safety net for all unhandled exceptions. The internal message is intentionally
     * suppressed from the response body to prevent accidental information disclosure.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(
            Exception ex,
            WebRequest request) {

        log.error("Unexpected error occurred", ex);
        return problemResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred. Please try again later.",
                request);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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

    /**
     * Value record representing a single field-level validation violation,
     * embedded in the {@code violations} property of 400 responses.
     */
    public record FieldViolation(String field, String message) {}
}
