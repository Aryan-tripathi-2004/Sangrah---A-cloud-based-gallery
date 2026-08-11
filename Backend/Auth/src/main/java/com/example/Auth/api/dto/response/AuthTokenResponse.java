package com.example.Auth.api.dto.response;

import com.example.Auth.api.dto.UserDTO;

/**
 * Immutable authentication token response.
 * Returned after successful login or registration.
 *
 * <p>{@code token} is the short-lived access token (matches frontend contract).
 * {@code refreshToken} is used to obtain new access tokens via /token/refresh.
 * {@code user} contains the authenticated user's public profile.
 */
public record AuthTokenResponse(
        String token,          // Short-lived access token — named 'token' to match frontend contract
        String refreshToken,
        UserDTO user
) {}
