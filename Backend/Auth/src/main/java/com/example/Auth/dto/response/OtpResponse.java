package com.example.Auth.dto.response;

import java.time.Instant;

/**
 * Response DTO for OTP generation.
 */
public class OtpResponse {

    private boolean sent;
    private String email;
    private Instant expiresAt;
    private int remainingAttempts;
    private String message;

    // Constructors
    public OtpResponse() {
    }

    public OtpResponse(boolean sent, String message) {
        this.sent = sent;
        this.message = message;
    }

    // Getters and Setters
    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    public void setRemainingAttempts(int remainingAttempts) {
        this.remainingAttempts = remainingAttempts;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "OtpResponse{" +
                "sent=" + sent +
                ", email='" + email + '\'' +
                ", expiresAt=" + expiresAt +
                ", remainingAttempts=" + remainingAttempts +
                ", message='" + message + '\'' +
                '}';
    }
}
