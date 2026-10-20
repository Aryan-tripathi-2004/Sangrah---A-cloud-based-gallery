package com.example.Media.api.dto.response;

import java.time.Instant;

public record StorageUsageLedgerResponse(
        String id,
        String userId,
        String mediaId,
        Long sizeBytes,
        Instant recordedAt,
        Instant deletedAt
) {
}
