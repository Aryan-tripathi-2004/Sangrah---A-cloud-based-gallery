package com.example.Auth.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard error response structure for API errors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private String code;
    private int httpStatus;
    private String userMessage;
    private String developerMessage;
    private String traceId;
    private Instant timestamp;
    private List<ValidationError> validationErrors;

    public ApiError() {
        this.timestamp = Instant.now();
    }

    private ApiError(Builder builder) {
        this.code = builder.code;
        this.httpStatus = builder.httpStatus;
        this.userMessage = builder.userMessage;
        this.developerMessage = builder.developerMessage;
        this.traceId = builder.traceId;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.validationErrors = builder.validationErrors;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getDeveloperMessage() {
        return developerMessage;
    }

    public void setDeveloperMessage(String developerMessage) {
        this.developerMessage = developerMessage;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }

    /**
     * Builder for ApiError.
     */
    public static class Builder {
        private String code;
        private int httpStatus;
        private String userMessage;
        private String developerMessage;
        private String traceId;
        private Instant timestamp;
        private List<ValidationError> validationErrors;

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder httpStatus(int httpStatus) {
            this.httpStatus = httpStatus;
            return this;
        }

        public Builder userMessage(String userMessage) {
            this.userMessage = userMessage;
            return this;
        }

        public Builder developerMessage(String developerMessage) {
            this.developerMessage = developerMessage;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder validationErrors(List<ValidationError> validationErrors) {
            this.validationErrors = validationErrors;
            return this;
        }

        public Builder addValidationError(String field, String message) {
            if (this.validationErrors == null) {
                this.validationErrors = new ArrayList<>();
            }
            this.validationErrors.add(new ValidationError(field, message));
            return this;
        }

        public ApiError build() {
            return new ApiError(this);
        }
    }

    /**
     * Represents a validation error for a specific field.
     */
    public static class ValidationError {
        private String field;
        private String message;

        public ValidationError() {
        }

        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
