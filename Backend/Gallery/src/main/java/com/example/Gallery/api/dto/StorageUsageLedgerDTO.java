package com.example.Gallery.api.dto;

import com.example.Gallery.shared.enums.MediaDomain;
import java.time.Instant;

/**
 * DTO for exposing storage usage ledger data via REST API
 * Used by Billing service to query ledger entries via Feign client
 * Allows Gallery service to own and manage its own data
 */
public record StorageUsageLedgerDTO(
    String id,
    String userId,
    MediaDomain domain,
    String domainRefId,
    long sizeBytes,
    Instant startAt,
    Instant endAt,
    String sourceService,
    Instant createdAt
) {}
