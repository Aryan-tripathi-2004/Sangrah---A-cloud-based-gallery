package com.example.Auth.common.exception;

import com.example.Auth.common.constants.ErrorConstants;

/**
 * Exception thrown when a conflict occurs (e.g., duplicate resource).
 */
public class ConflictException extends SangrahException {

    public ConflictException(String message) {
        super(ErrorConstants.ERR_CONFLICT, message, 409);
    }

    public ConflictException(String code, String message) {
        super(code, message, 409);
    }

    public ConflictException(String message, Throwable cause) {
        super(ErrorConstants.ERR_CONFLICT, message, 409, cause);
    }
}
