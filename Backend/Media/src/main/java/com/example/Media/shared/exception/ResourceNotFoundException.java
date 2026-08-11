package com.example.Media.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested {@code MediaDocument} or related resource cannot be found
 * in the persistence layer, or has been soft-deleted.
 *
 * <p>Caught by {@link GlobalExceptionHandler} and mapped to a
 * {@code 404 Not Found} RFC-9457 {@code ProblemDetail} response.</p>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
