package com.example.Auth.common.exception;

import com.example.Auth.common.constants.ErrorConstants;

/**
 * Exception thrown for internal server errors.
 */
public class InternalServerException extends SangrahException {

    public InternalServerException(String message) {
        super(ErrorConstants.ERR_INTERNAL_SERVER, message, 500);
    }

    public InternalServerException(String message, Throwable cause) {
        super(ErrorConstants.ERR_INTERNAL_SERVER, message, 500, cause);
    }
}
