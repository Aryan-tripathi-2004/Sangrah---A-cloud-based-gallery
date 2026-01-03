package com.example.Auth.common.exception;

import com.example.Auth.common.constants.ErrorConstants;

/**
 * Exception thrown when validation fails.
 */
public class ValidationException extends SangrahException {

    public ValidationException(String message) {
        super(ErrorConstants.ERR_VALIDATION_FAILED, message, 400);
    }

    public ValidationException(String message, Throwable cause) {
        super(ErrorConstants.ERR_VALIDATION_FAILED, message, 400, cause);
    }
}
