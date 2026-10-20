package com.example.Auth.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Immutable representation of a user's public profile data.
 * Used in API responses — never exposes passwordHash or internal fields.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDTO(
        String id,
        String email,
        String displayName,
        String createdAt,
        String bio,
        String avatarUrl
) {
    public UserDTO(String id, String email, String displayName, String createdAt) {
        this(id, email, displayName, createdAt, null, null);
    }
}
