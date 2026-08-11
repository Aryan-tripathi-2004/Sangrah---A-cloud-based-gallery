package com.example.Gallery.api.dto.response;

import com.example.Gallery.shared.enums.MediaType;
import com.example.Gallery.shared.enums.Visibility;

import java.time.Instant;
import java.util.Map;

/**
 * Response for a single media item
 */
public record MediaItemResponse(
    String id,
    String originalFileName,
    String mimeType,
    long sizeBytes,
    MediaType type,
    String checksumSha256,
    Map<String, Object> metadata,
    Instant uploadedAt,
    Instant deletedAt,
    Visibility visibilityStatus
) {}
