package com.example.Billing.shared.exception;

import com.stripe.exception.SignatureVerificationException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for Billing service.
 * Provides consistent error responses using RFC-9457 ProblemDetail.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getBindingResult().getErrorCount());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.sangrah.com/errors/validation-failed"));
        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailureException(OptimisticLockingFailureException ex) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The resource was modified by another transaction. Please retry.");
        problemDetail.setTitle("Conflict");
        problemDetail.setType(URI.create("https://api.sangrah.com/errors/conflict"));

        return problemDetail;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        log.warn("Missing request header: {}", ex.getHeaderName());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Missing required header: " + ex.getHeaderName());
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.sangrah.com/errors/missing-header"));

        return problemDetail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Not Found");
        problemDetail.setType(URI.create("https://api.sangrah.com/errors/not-found"));
        return problemDetail;
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbiddenOperationException(ForbiddenOperationException ex) {
        log.warn("Forbidden operation: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problemDetail.setTitle("Forbidden");
        problemDetail.setType(URI.create("https://api.sangrah.com/errors/forbidden"));
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.sangrah.com/errors/bad-request"));
        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.sangrah.com/errors/validation-failed"));
        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Unreadable or invalid message: {}", ex.getMessage());
        Throwable rootCause = ex.getMostSpecificCause();
        String detail = (rootCause != null && rootCause.getMessage() != null)
                ? rootCause.getMessage()
                : "Malformed or invalid request payload";
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problemDetail.setTitle("Invalid Request Body");
        problemDetail.setType(URI.create("https://api.sangrah.com/errors/invalid-body"));
        return problemDetail;
    }

    @ExceptionHandler(BillingProcessingException.class)
    public ProblemDetail handleBillingProcessingException(BillingProcessingException ex) {
        log.error("Billing processing error: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problemDetail.setTitle("Billing Processing Error");
        problemDetail.setType(URI.create("https://api.sangrah.com/errors/billing-error"));
        return problemDetail;
    }

    @ExceptionHandler(SignatureVerificationException.class)
    public ProblemDetail handleSignatureVerificationException(SignatureVerificationException ex) {
        log.error("❌ Webhook signature validation failed", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Invalid webhook signature");
        problemDetail.setTitle("Forbidden");
        problemDetail.setType(URI.create("https://api.sangrah.com/errors/forbidden"));
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGlobalException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.sangrah.com/errors/internal-server-error"));

        return problemDetail;
    }
}
