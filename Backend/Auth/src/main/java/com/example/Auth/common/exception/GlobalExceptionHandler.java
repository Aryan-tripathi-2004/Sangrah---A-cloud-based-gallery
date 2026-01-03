package com.example.Auth.common.exception;

import com.example.Auth.common.constants.ErrorConstants;
import com.example.Auth.common.dto.ApiError;
import com.example.Auth.common.logging.DashLogger;
import com.example.Auth.common.logging.DashLoggerFactory;
import com.example.Auth.common.request.RequestContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for all REST controllers.
 * Maps exceptions to standardized ApiError responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DashLogger logger = DashLoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle SangrahException and its subclasses.
     */
    @ExceptionHandler(SangrahException.class)
    public ResponseEntity<ApiError> handleSangrahException(SangrahException ex, WebRequest request) {
        String traceId = RequestContext.getCorrelationId();

        logger.error("Sangrah exception occurred", ex);

        ApiError error = ApiError.builder()
                .code(ex.getCode())
                .httpStatus(ex.getHttpStatus())
                .userMessage(ex.getMessage())
                .developerMessage(ex.getClass().getSimpleName() + ": " + ex.getMessage())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    /**
     * Handle validation exceptions from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String traceId = RequestContext.getCorrelationId();

        logger.warn("Validation failed", ex);

        List<ApiError.ValidationError> validationErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(new ApiError.ValidationError(error.getField(), error.getDefaultMessage()));
        }

        ApiError error = ApiError.builder()
                .code(ErrorConstants.ERR_VALIDATION_FAILED)
                .httpStatus(HttpStatus.BAD_REQUEST.value())
                .userMessage(ErrorConstants.MSG_VALIDATION_FAILED)
                .developerMessage("Validation failed with " + validationErrors.size() + " errors")
                .traceId(traceId)
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle constraint violation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(ConstraintViolationException ex,
            WebRequest request) {
        String traceId = RequestContext.getCorrelationId();

        logger.warn("Constraint violation", ex);

        List<ApiError.ValidationError> validationErrors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            validationErrors.add(new ApiError.ValidationError(field, message));
        }

        ApiError error = ApiError.builder()
                .code(ErrorConstants.ERR_VALIDATION_FAILED)
                .httpStatus(HttpStatus.BAD_REQUEST.value())
                .userMessage(ErrorConstants.MSG_VALIDATION_FAILED)
                .developerMessage("Constraint violations detected")
                .traceId(traceId)
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle Spring Security authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        String traceId = RequestContext.getCorrelationId();

        logger.warn("Authentication failed: " + ex.getMessage());

        ApiError error = ApiError.builder()
                .code(ErrorConstants.ERR_AUTH_UNAUTHORIZED)
                .httpStatus(HttpStatus.UNAUTHORIZED.value())
                .userMessage(ErrorConstants.MSG_AUTH_UNAUTHORIZED)
                .developerMessage(ex.getMessage())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle Spring Security access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        String traceId = RequestContext.getCorrelationId();

        logger.warn("Access denied: " + ex.getMessage());

        ApiError error = ApiError.builder()
                .code(ErrorConstants.ERR_AUTH_FORBIDDEN)
                .httpStatus(HttpStatus.FORBIDDEN.value())
                .userMessage(ErrorConstants.MSG_AUTH_FORBIDDEN)
                .developerMessage(ex.getMessage())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle method argument type mismatch.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatchException(MethodArgumentTypeMismatchException ex,
            WebRequest request) {
        String traceId = RequestContext.getCorrelationId();

        logger.warn("Type mismatch for parameter: " + ex.getName());

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ApiError error = ApiError.builder()
                .code(ErrorConstants.ERR_BAD_REQUEST)
                .httpStatus(HttpStatus.BAD_REQUEST.value())
                .userMessage(ErrorConstants.MSG_BAD_REQUEST)
                .developerMessage(message)
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, WebRequest request) {
        String traceId = RequestContext.getCorrelationId();

        logger.error("Unhandled exception occurred", ex);

        ApiError error = ApiError.builder()
                .code(ErrorConstants.ERR_INTERNAL_SERVER)
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .userMessage(ErrorConstants.MSG_INTERNAL_SERVER)
                .developerMessage(ex.getClass().getSimpleName() + ": " + ex.getMessage())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
