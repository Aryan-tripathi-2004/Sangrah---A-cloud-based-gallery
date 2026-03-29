package com.example.MediaService.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "storage_usage_ledger")
public class StorageUsageLedgerDocument {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String domain;  // "GALLERY", "EVENTS", "PROFILE_AVATAR", "MESSAGING"

    @Indexed
    private String domainRefId;  // Reference to media ID

    private Long sizeBytes;

    @Indexed
    private Instant startAt;  // When file uploaded

    private Instant endAt;  // null = active, set = when deleted (for billing)

    private String sourceService;  // "MediaService"

    private Instant createdAt;

    // Helper method to calculate days active for billing
    public long getDaysActive() {
        Instant effectiveEnd = endAt != null ? endAt : Instant.now();
        return ChronoUnit.DAYS.between(startAt, effectiveEnd) + 1;  // +1 to include start day
    }

    // Helper method to calculate byte-days for billing
    public double getBytesDays() {
        return sizeBytes * getDaysActive();
    }

    // Check if entry is still active
    public boolean isActive() {
        return endAt == null;
    }
}
