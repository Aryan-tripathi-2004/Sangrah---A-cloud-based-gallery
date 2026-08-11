package com.example.Auth.api.dto.response;

import com.example.Auth.api.dto.UserDTO;

/**
 * Strongly-typed wrapper response for User endpoints.
 * Replaces raw {@code Map<String, Object>} returns in UserController,
 * providing a consistent, typed API contract.
 *
 * @param data    the user payload — may be {@code null} for non-data responses
 * @param status  the response status string (e.g., "success")
 * @param message an optional human-readable message
 */
public record UserWrapperResponse(
        UserDTO data,
        String status,
        String message
) {}
