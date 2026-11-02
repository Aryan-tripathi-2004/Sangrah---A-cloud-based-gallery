package com.example.Media.api.dto.response;

import com.example.Media.shared.enums.MediaDomain;

import java.time.Instant;

public record StorageUsageLedgerResponse(
        String id,
        String userId,
        MediaDomain domain,
        String domainRefId,
        Long sizeBytes,
        Instant startAt,
        Instant endAt,
        String sourceService,
        Instant createdAt,
        String mediaId,
        Instant recordedAt,
        Instant deletedAt
) {
    public StorageUsageLedgerResponse(
            String id,
            String userId,
            MediaDomain domain,
            String domainRefId,
            Long sizeBytes,
            Instant startAt,
            Instant endAt,
            String sourceService,
            Instant createdAt) {
        this(id, userId, domain, domainRefId, sizeBytes, startAt, endAt, sourceService, createdAt,
                domainRefId, startAt, endAt);
    }
}
