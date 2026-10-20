package com.example.Gallery.api.dto.response;

import lombok.Builder;

/**
 * Response showing current storage usage matching the frontend API contract.
 */
@Builder
public record StorageUsageResponse(
    long totalBytesUsed,
    long fileCount,
    StorageBreakdown breakdown,
    StorageFormatted formatted,
    String message
) {
    @Builder
    public record StorageBreakdown(
        long imageBytes,
        long videoBytes
    ) {}

    @Builder
    public record StorageFormatted(
        String totalFormatted,
        String imageFormatted,
        String videoFormatted
    ) {}
}
