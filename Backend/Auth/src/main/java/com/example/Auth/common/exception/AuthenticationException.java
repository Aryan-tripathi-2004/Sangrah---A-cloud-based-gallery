package com.example.Auth.common.exception;

import com.example.Auth.common.constants.ErrorConstants;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends SangrahException {

    public AuthenticationException(String message) {
        super(ErrorConstants.ERR_AUTH_UNAUTHORIZED, message, 401);
    }

    public AuthenticationException(String code, String message) {
        super(code, message, 401);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(ErrorConstants.ERR_AUTH_UNAUTHORIZED, message, 401, cause);
    }

    public AuthenticationException(String code, String message, Throwable cause) {
        super(code, message, 401, cause);
    }
}
