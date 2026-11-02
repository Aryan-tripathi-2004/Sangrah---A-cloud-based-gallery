package com.example.Gallery.api.dto.response;

/**
 * Response DTO for media soft-deletion in Gallery service.
 */
public record MediaDeleteResponse(
    String message,
    String mediaId
) {}
