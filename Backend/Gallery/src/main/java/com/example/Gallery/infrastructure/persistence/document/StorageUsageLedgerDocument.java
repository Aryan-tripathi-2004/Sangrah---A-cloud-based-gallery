package com.example.Gallery.infrastructure.persistence.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * CRITICAL FOR PHASE 4 BILLING:
 *
 * This document tracks every file upload and deletion for accurate prorated billing.
 * When Phase 4 (Billing) runs, it will:
 * 1. Read all entries from this collection for a user
 * 2. Calculate byte-days: sum(file_size * days_existed)
 * 3. Calculate cost: byte_days * rate_per_MB_per_day
 *
 * Storage is tracked separately from Event storage (billing shows: "Gallery: $X, Events: $Y")
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("storage_usage_ledger")
public class StorageUsageLedgerDocument {
    @Id
    private String id;

    @Indexed
    private String userId;  // indexed for billing queries

    private String domain;  // e.g., "GALLERY", "EVENTS"
    private String domainRefId;  // reference to media/event ID
    private long sizeBytes;  // file size in bytes

    private Instant startAt;  // when file was uploaded
    private Instant endAt;  // null = still active (user hasn't deleted it)

    private String sourceService;  // "Gallery", "Event", etc.

    private Instant createdAt;  // when ledger entry created

    // Helper method
    public boolean isActive() {
        return endAt == null;
    }

    // Helper method for billing calculation
    public long getDaysActive(Instant endDate) {
        Instant effectiveEndDate = endAt != null ? endAt : endDate;
        return java.time.temporal.ChronoUnit.DAYS.between(startAt, effectiveEndDate);
    }
}

