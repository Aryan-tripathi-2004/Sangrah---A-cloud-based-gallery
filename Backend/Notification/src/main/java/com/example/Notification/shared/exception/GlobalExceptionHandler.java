package com.example.Notification.shared.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for Notification service using RFC-9457 ProblemDetail.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${sangrah.api.error-type-base-url}")
    private String errorTypeBaseUrl;

    private URI createErrorTypeUri(String errorSlug) {
        return URI.create(errorTypeBaseUrl + "/" + errorSlug);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation error in Notification service: {}", ex.getBindingResult().getErrorCount());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(createErrorTypeUri("validation-failed"));
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

        return problemDetail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation in Notification service: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        for (var violation : ex.getConstraintViolations()) {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setTitle("Constraint Violation");
        problemDetail.setType(createErrorTypeUri("constraint-violation"));
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));
        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Unreadable request body in Notification service: {}", ex.getMessage());
        Throwable rootCause = ex.getMostSpecificCause();
        String detail = (rootCause != null && rootCause.getMessage() != null)
                ? rootCause.getMessage()
                : "Malformed or invalid request body";
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problemDetail.setTitle("Invalid Request Body");
        problemDetail.setType(createErrorTypeUri("invalid-body"));
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));
        return problemDetail;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingRequestHeaderException(MissingRequestHeaderException ex, WebRequest request) {
        log.warn("Missing request header: {}", ex.getHeaderName());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problemDetail.setTitle("Unauthorized");
        problemDetail.setType(createErrorTypeUri("missing-header"));
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

        return problemDetail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Not Found");
        problemDetail.setType(createErrorTypeUri("resource-not-found"));
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

        return problemDetail;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailureException(OptimisticLockingFailureException ex, WebRequest request) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The resource was updated by another request. Please try again.");
        problemDetail.setTitle("Conflict");
        problemDetail.setType(createErrorTypeUri("conflict"));
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument in Notification service: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(createErrorTypeUri("invalid-argument"));
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

        return problemDetail;
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ProblemDetail handlePermissionDeniedException(PermissionDeniedException ex) {
        log.warn("Permission denied: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problemDetail.setTitle("Permission Denied");
        problemDetail.setType(createErrorTypeUri("forbidden"));
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred in Notification service", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(createErrorTypeUri("internal-server-error"));
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

        return problemDetail;
    }
}
