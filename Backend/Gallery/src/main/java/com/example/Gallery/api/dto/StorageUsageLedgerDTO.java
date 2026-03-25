package com.example.Gallery.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for exposing storage usage ledger data via REST API
 * Used by Billing service to query ledger entries via Feign client
 * Allows Gallery service to own and manage its own data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageUsageLedgerDTO {
    private String id;
    private String userId;
    private String domain;  // "GALLERY"
    private String domainRefId;  // reference to media ID
    private long sizeBytes;
    private Instant startAt;  // upload date
    private Instant endAt;  // delete date (null if active)
    private String sourceService;
    private Instant createdAt;
}
