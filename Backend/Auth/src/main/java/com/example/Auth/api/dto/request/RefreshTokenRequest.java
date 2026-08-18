package com.example.Auth.api.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Immutable refresh token request payload.
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
