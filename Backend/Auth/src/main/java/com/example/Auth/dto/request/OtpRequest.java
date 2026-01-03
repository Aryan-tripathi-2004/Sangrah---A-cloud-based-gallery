package com.example.Auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for generating OTP.
 */
public class OtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Purpose is required")
    private Purpose purpose;

    public enum Purpose {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        TWO_FACTOR_AUTH,
        ACCOUNT_RECOVERY
    }

    // Constructors
    public OtpRequest() {
    }

    public OtpRequest(String email, Purpose purpose) {
        this.email = email;
        this.purpose = purpose;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public void setPurpose(Purpose purpose) {
        this.purpose = purpose;
    }

    @Override
    public String toString() {
        return "OtpRequest{" +
                "email='" + email + '\'' +
                ", purpose=" + purpose +
                '}';
    }
}
