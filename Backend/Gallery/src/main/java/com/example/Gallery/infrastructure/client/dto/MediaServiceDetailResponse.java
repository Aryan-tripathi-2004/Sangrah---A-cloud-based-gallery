package com.example.Gallery.infrastructure.client.dto;

import java.time.Instant;

public record MediaServiceDetailResponse(
    String id,
    String storageKey,
    String originalFileName,
    String mimeType,
    long sizeBytes,
    String checksumSha256,
    String domain,
    String status,
    Instant uploadedAt
) {}
