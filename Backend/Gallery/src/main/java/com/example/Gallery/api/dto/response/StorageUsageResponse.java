package com.example.Gallery.api.dto.response;

/**
 * Response showing current storage usage (informational, NOT a quota limit)
 * Pay-as-you-use model: users can upload unlimited, only charged for what they use
 */
public record StorageUsageResponse(
    long totalBytesUsed,
    long fileCount,
    long imageBytes,
    long videoBytes,
    String oldestFile,
    String newestFile,
    String formattedUsage
) {}
