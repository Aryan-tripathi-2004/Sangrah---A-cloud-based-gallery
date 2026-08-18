package com.example.Gallery.infrastructure.client.dto;

import java.time.Instant;

public record MediaServiceUploadResponse(
    String id,
    String storageKey,
    String checksumSha256,
    String originalFileName,
    String mimeType,
    long sizeBytes,
    String domain,
    Instant uploadedAt
) {}
