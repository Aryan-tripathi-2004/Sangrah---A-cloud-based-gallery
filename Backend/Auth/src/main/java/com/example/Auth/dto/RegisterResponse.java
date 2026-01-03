package com.example.Auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Response DTO for user registration.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterResponse {

    private String userId;
    private String username;
    private String email;
    private String message;
    private boolean requiresEmailVerification;
    private Instant registeredAt;

    // Constructors

    public RegisterResponse() {
    }

    public RegisterResponse(String userId, String username, String email,
            String message, boolean requiresEmailVerification) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.message = message;
        this.requiresEmailVerification = requiresEmailVerification;
        this.registeredAt = Instant.now();
    }

    // Builder pattern for flexible construction

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private String username;
        private String email;
        private String message;
        private boolean requiresEmailVerification;
        private Instant registeredAt;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder requiresEmailVerification(boolean requiresEmailVerification) {
            this.requiresEmailVerification = requiresEmailVerification;
            return this;
        }

        public Builder registeredAt(Instant registeredAt) {
            this.registeredAt = registeredAt;
            return this;
        }

        public RegisterResponse build() {
            RegisterResponse response = new RegisterResponse();
            response.userId = this.userId;
            response.username = this.username;
            response.email = this.email;
            response.message = this.message;
            response.requiresEmailVerification = this.requiresEmailVerification;
            response.registeredAt = this.registeredAt != null ? this.registeredAt : Instant.now();
            return response;
        }
    }

    // Getters and Setters

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRequiresEmailVerification() {
        return requiresEmailVerification;
    }

    public void setRequiresEmailVerification(boolean requiresEmailVerification) {
        this.requiresEmailVerification = requiresEmailVerification;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public String toString() {
        return "RegisterResponse{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", message='" + message + '\'' +
                ", requiresEmailVerification=" + requiresEmailVerification +
                ", registeredAt=" + registeredAt +
                '}';
    }
}
