package com.example.Email.shared.exception;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralised RFC-9457 error-response factory for the Email microservice.
 *
 * <p>This advice intercepts every exception that escapes a controller method and
 * converts it into a {@link ProblemDetail} response (Spring Boot 3 native support).
 * No controller method needs a try-catch block – all exception handling lives here.
 *
 * <p>Handled exception hierarchy:
 * <ol>
 *   <li>{@link MethodArgumentNotValidException} – Jakarta Bean Validation failures → 422</li>
 *   <li>{@link MessagingException}              – SMTP transport errors          → 502</li>
 *   <li>{@link ResourceNotFoundException}       – 404 look-up misses             → 404</li>
 *   <li>{@link Exception}                       – catch-all safety net           → 500
 *       (message suppressed to avoid leaking internals to callers)</li>
 * </ol>
 *
 * <p>Every {@link ProblemDetail} is enriched with:
 * <ul>
 *   <li>{@code timestamp} – UTC instant of the error</li>
 *   <li>{@code service}   – identifies this microservice for distributed tracing</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI VALIDATION_TYPE  = URI.create("https://sangrah.com/problems/validation-error");
    private static final URI SMTP_TYPE        = URI.create("https://sangrah.com/problems/smtp-error");
    private static final URI NOT_FOUND_TYPE   = URI.create("https://sangrah.com/problems/resource-not-found");
    private static final URI INTERNAL_TYPE    = URI.create("https://sangrah.com/problems/internal-error");

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Validation failures (Jakarta Bean Validation)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Handle {@link MethodArgumentNotValidException} raised when {@code @Valid}
     * constraint checking fails on a request body record.
     *
     * <p>Returns HTTP {@code 422 Unprocessable Entity} with a list of
     * field-level violation messages in the {@code violations} extension property.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("⚠️ [GLOBAL HANDLER] Validation failure: {}", ex.getMessage());

        List<String> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "One or more request fields failed validation."
        );
        problem.setType(VALIDATION_TYPE);
        problem.setTitle("Validation Error");
        problem.setProperty("violations", violations);
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("service", "email-service");
        return problem;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. SMTP / Messaging failures
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Handle {@link MessagingException} propagated by {@code EmailServiceImpl}
     * when the SMTP transport layer rejects or cannot deliver a message.
     *
     * <p>Returns HTTP {@code 502 Bad Gateway} – the upstream mail server is the
     * failing dependency, not the service itself.
     */
    @ExceptionHandler(MessagingException.class)
    public ProblemDetail handleMessagingException(MessagingException ex) {
        log.error("❌ [GLOBAL HANDLER] SMTP MessagingException: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "The email could not be dispatched due to an SMTP transport error. "
                + "Please try again later."
        );
        problem.setType(SMTP_TYPE);
        problem.setTitle("SMTP Delivery Error");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("service", "email-service");
        return problem;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Resource not found
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Handle {@link ResourceNotFoundException} thrown when an email log entry
     * or other entity cannot be located in MongoDB.
     *
     * <p>Returns HTTP {@code 404 Not Found}.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("⚠️ [GLOBAL HANDLER] Resource not found: {}", ex.getReason());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getReason()
        );
        problem.setType(NOT_FOUND_TYPE);
        problem.setTitle("Resource Not Found");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("service", "email-service");
        return problem;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Catch-all safety net
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Catch-all handler for every unhandled {@link Exception} subclass.
     *
     * <p>Returns HTTP {@code 500 Internal Server Error}. The exception message
     * is <strong>intentionally suppressed</strong> in the response body to prevent
     * stack-trace or implementation detail leakage to external callers. The full
     * exception is still logged at ERROR level for internal diagnostics.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("❌ [GLOBAL HANDLER] Unhandled exception: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support if the problem persists."
        );
        problem.setType(INTERNAL_TYPE);
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("service", "email-service");
        return problem;
    }
}
