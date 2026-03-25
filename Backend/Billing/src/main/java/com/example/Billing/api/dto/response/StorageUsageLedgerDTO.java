package com.example.Billing.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for Storage Usage Ledger data received from Gallery service
 * This is used to receive storage usage data via Feign client
 * from the Gallery microservice
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageUsageLedgerDTO {
    private String id;
    private String userId;
    private String domain;  // "GALLERY", "EVENTS", etc.
    private String domainRefId;  // reference to media/event ID
    private long sizeBytes;  // file size in bytes
    private Instant startAt;  // upload/creation date
    private Instant endAt;  // delete date (null if still active)
    private String sourceService;  // "Gallery", "Event", etc.
    private Instant createdAt;  // when ledger entry was created
}
