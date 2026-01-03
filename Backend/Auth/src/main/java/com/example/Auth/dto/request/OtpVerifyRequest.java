package com.example.Auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for verifying OTP.
 */
public class OtpVerifyRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP code must be 6 digits")
    private String otpCode;

    @NotNull(message = "Purpose is required")
    private Purpose purpose;

    public enum Purpose {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        TWO_FACTOR_AUTH,
        ACCOUNT_RECOVERY
    }

    // Constructors
    public OtpVerifyRequest() {
    }

    public OtpVerifyRequest(String email, String otpCode, Purpose purpose) {
        this.email = email;
        this.otpCode = otpCode;
        this.purpose = purpose;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public void setPurpose(Purpose purpose) {
        this.purpose = purpose;
    }

    @Override
    public String toString() {
        return "OtpVerifyRequest{" +
                "email='" + email + '\'' +
                ", otpCode='[PROTECTED]'" +
                ", purpose=" + purpose +
                '}';
    }
}
