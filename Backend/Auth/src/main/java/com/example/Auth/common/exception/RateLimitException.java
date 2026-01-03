package com.example.Auth.common.exception;

import com.example.Auth.common.constants.ErrorConstants;

/**
 * Exception thrown when rate limit is exceeded.
 */
public class RateLimitException extends SangrahException {

    private final long retryAfterSeconds;

    public RateLimitException(String message, long retryAfterSeconds) {
        super(ErrorConstants.ERR_RATE_LIMIT_EXCEEDED, message, 429);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitException(String code, String message, long retryAfterSeconds) {
        super(code, message, 429);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
