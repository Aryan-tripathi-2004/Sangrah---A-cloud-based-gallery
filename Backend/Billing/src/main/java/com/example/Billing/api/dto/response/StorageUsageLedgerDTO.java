package com.example.Billing.api.dto.response;

import com.example.Billing.shared.enums.MediaDomain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.time.Instant;

/**
 * DTO for Storage Usage Ledger data received from Media / Gallery service
 * via Feign client.
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
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
