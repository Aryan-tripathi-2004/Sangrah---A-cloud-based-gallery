package com.example.Auth.common.exception;

import com.example.Auth.common.constants.ErrorConstants;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends SangrahException {

    public ResourceNotFoundException(String message) {
        super(ErrorConstants.ERR_RESOURCE_NOT_FOUND, message, 404);
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(ErrorConstants.ERR_RESOURCE_NOT_FOUND,
                String.format("%s not found with identifier: %s", resourceType, identifier),
                404);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(ErrorConstants.ERR_RESOURCE_NOT_FOUND, message, 404, cause);
    }
}
