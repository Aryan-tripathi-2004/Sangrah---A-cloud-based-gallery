package com.example.Auth.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Immutable login request payload.
 */
public record LoginRequest(
        @Email(message = "Must be a valid email address")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}
