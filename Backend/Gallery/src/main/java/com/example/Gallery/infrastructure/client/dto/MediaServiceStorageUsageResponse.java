package com.example.Gallery.infrastructure.client.dto;

public record MediaServiceStorageUsageResponse(
        String userId,
        long usedBytes,
        long limitBytes,
        double percentageUsed
) {}
