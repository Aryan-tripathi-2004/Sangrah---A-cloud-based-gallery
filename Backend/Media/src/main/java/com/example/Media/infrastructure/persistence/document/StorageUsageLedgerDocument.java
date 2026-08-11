package com.example.Media.infrastructure.persistence.document;

import com.example.Media.shared.enums.MediaDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * MongoDB document that acts as an immutable-by-convention audit ledger for
 * tracking per-user, per-domain storage consumption over time.
 *
 * <p>Design notes:
 * <ul>
 *   <li>{@code @Data} is intentionally replaced by granular Lombok annotations for the
 *       same reasons as {@code MediaDocument}: entity identity is defined by {@code @Id},
 *       not by field equality.</li>
 *   <li>{@code @Version} enables optimistic locking to prevent double-close race conditions
 *       on the {@code endAt} field when concurrent delete requests arrive simultaneously.</li>
 *   <li>{@code domain} is typed as {@link MediaDomain}. MongoDB stores the enum name string,
 *       so existing ledger documents remain fully backward-compatible.</li>
 * </ul>
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "storage_usage_ledger")
public class StorageUsageLedgerDocument {

    @Id
    private String id;

    @Indexed
    private String userId;

    /** Business domain that owns the associated media asset. */
    @Indexed
    private MediaDomain domain;

    /** Reference to the {@link MediaDocument#getId()} this ledger entry tracks. */
    @Indexed
    private String domainRefId;

    private Long sizeBytes;

    /** Timestamp when the file was uploaded (inclusive billing start). */
    @Indexed
    private Instant startAt;

    /** {@code null} while the file is active; set to deletion timestamp when soft-deleted. */
    private Instant endAt;

    private String sourceService;

    private Instant createdAt;

    /**
     * Optimistic locking version field managed by Spring Data MongoDB.
     * Guards against concurrent race conditions when closing a ledger entry.
     */
    @Version
    private Long version;

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
