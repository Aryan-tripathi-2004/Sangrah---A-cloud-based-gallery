package com.example.Auth.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Immutable user registration request payload.
 */
public record RegisterRequest(
        @Email(message = "Must be a valid email address")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Display name is required")
        String displayName
) {}
