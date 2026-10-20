package com.example.Media.api.dto.response;

public record StorageUsageResponse(
        String userId,
        long usedBytes,
        long limitBytes,
        double percentageUsed
) {
}
