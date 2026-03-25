package com.example.Billing.infrastructure.persistence.repository;

import com.example.Billing.infrastructure.persistence.document.StorageUsageLedgerDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for StorageUsageLedger persistence in MongoDB.
 */
@Repository
public interface StorageUsageLedgerRepository extends MongoRepository<StorageUsageLedgerDocument, String> {
    /**
     * Find ledger entries by user ID
     */
    List<StorageUsageLedgerDocument> findByUserId(String userId);

    /**
     * Find ledger entries by user and date range
     */
    List<StorageUsageLedgerDocument> findByUserIdAndCreatedAtBetween(String userId, Instant startDate, Instant endDate);

    /**
     * Find ledger entries created after a specific date for a user
     */
    List<StorageUsageLedgerDocument> findByUserIdAndCreatedAtGreaterThanEqual(String userId, Instant createdAt);

    /**
     * Find ledger entries by date range (for invoice generation)
     */
    List<StorageUsageLedgerDocument> findByStartAtBetween(Instant startDate, Instant endDate);
}
