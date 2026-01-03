package com.example.Auth.common.exception;

import com.example.Auth.common.constants.ErrorConstants;

/**
 * Exception thrown when an external service call fails.
 */
public class ExternalServiceException extends SangrahException {

    private final String serviceName;

    public ExternalServiceException(String serviceName, String message) {
        super(ErrorConstants.ERR_EXTERNAL_SERVICE,
                String.format("External service '%s' error: %s", serviceName, message),
                503);
        this.serviceName = serviceName;
    }

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super(ErrorConstants.ERR_EXTERNAL_SERVICE,
                String.format("External service '%s' error: %s", serviceName, message),
                503,
                cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
