package com.example.Auth.common.exception;

/**
 * Base exception for all business exceptions in the Sangrah platform.
 */
public class SangrahException extends RuntimeException {

    private final String code;
    private final int httpStatus;

    public SangrahException(String message) {
        super(message);
        this.code = "ERR_INTERNAL_SERVER";
        this.httpStatus = 500;
    }

    public SangrahException(String code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = 500;
    }

    public SangrahException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public SangrahException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = 500;
    }

    public SangrahException(String code, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
