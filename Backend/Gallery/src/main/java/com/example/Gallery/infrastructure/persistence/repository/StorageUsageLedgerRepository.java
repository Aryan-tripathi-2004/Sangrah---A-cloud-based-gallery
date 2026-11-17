package com.example.Gallery.infrastructure.persistence.repository;

import com.example.Gallery.infrastructure.persistence.document.StorageUsageLedgerDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for StorageUsageLedger persistence.
 *
 * CRITICAL FOR BILLING:
 * This repository is queried by Phase 4 (Billing Service) to calculate charges.
 * Every upload creates an entry here, every delete marks it with deletedDate.
 *
 * Billing calculation: byte-days = sum(sizeBytes * daysActive)
 */
@Repository
public interface StorageUsageLedgerRepository extends MongoRepository<StorageUsageLedgerDocument, String> {

    /**
     * Find all storage usage entries for a user (active AND deleted)
     * Used by billing to calculate monthly charges
     */
    List<StorageUsageLedgerDocument> findByUserId(String userId);

    /**
     * Find only ACTIVE storage entries for a user (endAt is null)
     * Shows current storage consumption
     */
    List<StorageUsageLedgerDocument> findByUserIdAndEndAtIsNull(String userId);

    /**
     * Find entries by domain ref ID (to track specific file)
     */
    List<StorageUsageLedgerDocument> findByDomainRefId(String domainRefId);

    /**
     * Overlapping period queries for accurate prorated billing (files active at any point in [startDate, endDate])
     */
    @Query("{ 'startAt': { $lte: ?1 }, $or: [ { 'endAt': null }, { 'endAt': { $gte: ?0 } } ] }")
    Page<StorageUsageLedgerDocument> findActiveLedgerInPeriod(Instant startDate, Instant endDate, Pageable pageable);

    @Query("{ 'startAt': { $lte: ?1 }, $or: [ { 'endAt': null }, { 'endAt': { $gte: ?0 } } ] }")
    List<StorageUsageLedgerDocument> findActiveLedgerInPeriod(Instant startDate, Instant endDate);

    @Query("{ 'userId': ?0, 'startAt': { $lte: ?2 }, $or: [ { 'endAt': null }, { 'endAt': { $gte: ?1 } } ] }")
    Page<StorageUsageLedgerDocument> findActiveLedgerInPeriodForUser(String userId, Instant startDate, Instant endDate, Pageable pageable);

    @Query("{ 'userId': ?0, 'startAt': { $lte: ?2 }, $or: [ { 'endAt': null }, { 'endAt': { $gte: ?1 } } ] }")
    List<StorageUsageLedgerDocument> findActiveLedgerInPeriodForUser(String userId, Instant startDate, Instant endDate);

    /**
     * Find entries within a date range (for month-end billing)
     * Includes both active and deleted files
     */
    List<StorageUsageLedgerDocument> findByStartAtBetween(Instant startDate, Instant endDate);

    /**
     * Paginated find entries within a date range (for unbounded data safe billing processing)
     */
    Page<StorageUsageLedgerDocument> findByStartAtBetween(Instant startDate, Instant endDate, Pageable pageable);

    /**
     * Find entries for a user within a date range
     */
    List<StorageUsageLedgerDocument> findByUserIdAndStartAtBetween(String userId, Instant startDate, Instant endDate);

    /**
     * Calculate total bytes currently used by user (all active files)
     * Used for storage usage endpoint
     */
    @Query(value = "{ 'userId': ?0, 'endAt': null }", fields = "{ 'sizeBytes': 1 }")
    List<StorageUsageLedgerDocument> findActiveStorageForUser(String userId);

    /**
     * Delete old ledger entries (keep for 30+ days after deletion)
     */
    long deleteByEndAtBefore(Instant purgeDate);
}
