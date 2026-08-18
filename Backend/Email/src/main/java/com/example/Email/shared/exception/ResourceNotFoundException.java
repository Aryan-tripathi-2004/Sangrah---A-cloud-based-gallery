package com.example.Email.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when a requested resource (e.g. an email log entry) cannot be located.
 *
 * <p>Extends {@link ResponseStatusException} so Spring MVC can extract the HTTP
 * status code without any special handling in the controller layer. The
 * {@link com.example.Email.shared.exception.GlobalExceptionHandler} intercepts this
 * exception and produces an RFC-9457 {@code ProblemDetail} response body.
 */
public class ResourceNotFoundException extends ResponseStatusException {

    /**
     * @param resourceName human-readable name of the resource type (e.g. "EmailLog")
     * @param identifier   the ID or key that was not found
     */
    public ResourceNotFoundException(String resourceName, String identifier) {
        super(
                HttpStatus.NOT_FOUND,
                String.format("%s not found with identifier: %s", resourceName, identifier)
        );
    }
}
