package com.example.Billing.api.dto.response;

import com.example.Billing.shared.enums.MediaDomain;
import lombok.Builder;

import java.time.Instant;

/**
 * DTO for Storage Usage Ledger data received from Gallery service
 * This is used to receive storage usage data via Feign client
 * from the Gallery microservice
 */
@Builder
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
