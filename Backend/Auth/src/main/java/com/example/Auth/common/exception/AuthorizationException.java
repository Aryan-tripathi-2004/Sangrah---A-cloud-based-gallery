package com.example.Auth.common.exception;

import com.example.Auth.common.constants.ErrorConstants;

/**
 * Exception thrown when authorization fails (user doesn't have permission).
 */
public class AuthorizationException extends SangrahException {

    public AuthorizationException(String message) {
        super(ErrorConstants.ERR_AUTH_FORBIDDEN, message, 403);
    }

    public AuthorizationException(String code, String message) {
        super(code, message, 403);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(ErrorConstants.ERR_AUTH_FORBIDDEN, message, 403, cause);
    }
}
