package com.example.Media.infrastructure.persistence.repository;

import com.example.Media.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Media.shared.enums.MediaDomain;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StorageUsageLedgerRepository extends MongoRepository<StorageUsageLedgerDocument, String> {

    // Find ledger entries for a user in a date range (for billing)
    List<StorageUsageLedgerDocument> findByUserIdAndStartAtBetween(String userId, Instant startDate, Instant endDate);

    // Find ledger entries for a user and domain in a date range
    List<StorageUsageLedgerDocument> findByUserIdAndDomainAndStartAtBetween(String userId, MediaDomain domain, Instant startDate, Instant endDate);

    // Find all active ledger entries for a user
    List<StorageUsageLedgerDocument> findByUserIdAndEndAtIsNull(String userId);

    // Find ledger entry by domain reference
    List<StorageUsageLedgerDocument> findByDomainRefId(String domainRefId);

    // Find all entries for a user across all domains
    List<StorageUsageLedgerDocument> findByUserId(String userId);

    // Find entries by domain
    List<StorageUsageLedgerDocument> findByDomain(MediaDomain domain);
}
