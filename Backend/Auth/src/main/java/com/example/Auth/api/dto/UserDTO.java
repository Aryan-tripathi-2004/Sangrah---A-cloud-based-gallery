package com.example.Auth.api.dto;

/**
 * Immutable representation of a user's public profile data.
 * Used in API responses — never exposes passwordHash or internal fields.
 */
public record UserDTO(
        String id,
        String email,
        String displayName,
        String createdAt
) {}
