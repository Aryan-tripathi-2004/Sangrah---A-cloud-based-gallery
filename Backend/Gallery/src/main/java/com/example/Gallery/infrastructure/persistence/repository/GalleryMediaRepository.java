package com.example.Gallery.infrastructure.persistence.repository;

import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for GalleryMedia persistence in MongoDB.
 * Supports pay-as-you-use model with no quotas.
 */
@Repository
public interface GalleryMediaRepository extends MongoRepository<GalleryMediaDocument, String> {
    /**
     * Find all non-deleted media by user ID (active files)
     */
    List<GalleryMediaDocument> findByUserIdAndDeletedAtIsNull(String userId);

    /**
     * Paginated find non-deleted media by user ID (active files)
     */
    Page<GalleryMediaDocument> findByUserIdAndDeletedAtIsNull(String userId, Pageable pageable);

    /**
     * Find all media by user (including deleted) for archive queries
     */
    List<GalleryMediaDocument> findByUserId(String userId);

    /**
     * Find media by owner user ID
     */
    List<GalleryMediaDocument> findByOwnerUserId(String ownerUserId);

    /**
     * Find media by owner and MIME type
     */
    List<GalleryMediaDocument> findByOwnerUserIdAndMimeType(String ownerUserId, String mimeType);

    /**
     * Find media by owner sorted by upload date descending
     */
    List<GalleryMediaDocument> findByOwnerUserIdOrderByUploadedAtDesc(String ownerUserId);

    /**
     * Find media by checksum (SHA-256) for deduplication detection
     */
    Optional<GalleryMediaDocument> findByUserIdAndChecksumSha256(String userId, String checksumSha256);

    /**
     * Find media by ID and verify ownership
     */
    Optional<GalleryMediaDocument> findByIdAndUserId(String id, String userId);

    /**
     * Calculate total storage used by user (non-deleted files only)
     */
    long countByUserIdAndDeletedAtIsNull(String userId);

    /**
     * Find all non-deleted files uploaded after a certain date
     */
    List<GalleryMediaDocument> findByUserIdAndUploadedAtGreaterThanAndDeletedAtIsNull(String userId, Instant uploadedAfter);

    /**
     * Find non-deleted files within a date range (for timeline views)
     */
    List<GalleryMediaDocument> findByUserIdAndUploadedAtBetweenAndDeletedAtIsNullOrderByUploadedAtDesc(
            String userId, Instant startDate, Instant endDate);

    /**
     * Paginated find non-deleted files within a date range (for timeline views)
     */
    Page<GalleryMediaDocument> findByUserIdAndUploadedAtBetweenAndDeletedAtIsNullOrderByUploadedAtDesc(
            String userId, Instant startDate, Instant endDate, Pageable pageable);

    /**
     * Push daily timeline grouping down to MongoDB engine using aggregation pipeline.
     */
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0, deletedAt: null } }",
        "{ $group: { _id: { $dateToString: { format: '%Y-%m-%d', date: '$uploadedAt' } }, count: { $sum: 1 } } }",
        "{ $sort: { '_id': -1 } }"
    })
    List<TimelineGroupAggregationResult> getGroupedTimelineDaily(String userId);

    /**
     * Paginated daily timeline grouping using MongoDB aggregation pipeline.
     */
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0, deletedAt: null } }",
        "{ $group: { _id: { $dateToString: { format: '%Y-%m-%d', date: '$uploadedAt' } }, count: { $sum: 1 } } }",
        "{ $sort: { '_id': -1 } }",
        "{ $skip: ?1 }",
        "{ $limit: ?2 }"
    })
    List<TimelineGroupAggregationResult> getGroupedTimelineDailyPaginated(String userId, long skip, int limit);

    /**
     * Count distinct daily timeline groups.
     */
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0, deletedAt: null } }",
        "{ $group: { _id: { $dateToString: { format: '%Y-%m-%d', date: '$uploadedAt' } } } }",
        "{ $count: 'total' }"
    })
    TimelineCountResult countGroupedTimelineDaily(String userId);

    /**
     * Push monthly timeline grouping down to MongoDB engine using aggregation pipeline.
     */
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0, deletedAt: null } }",
        "{ $group: { _id: { $dateToString: { format: '%Y-%m', date: '$uploadedAt' } }, count: { $sum: 1 } } }",
        "{ $sort: { '_id': -1 } }"
    })
    List<TimelineGroupAggregationResult> getGroupedTimelineMonthly(String userId);

    /**
     * Paginated monthly timeline grouping using MongoDB aggregation pipeline.
     */
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0, deletedAt: null } }",
        "{ $group: { _id: { $dateToString: { format: '%Y-%m', date: '$uploadedAt' } }, count: { $sum: 1 } } }",
        "{ $sort: { '_id': -1 } }",
        "{ $skip: ?1 }",
        "{ $limit: ?2 }"
    })
    List<TimelineGroupAggregationResult> getGroupedTimelineMonthlyPaginated(String userId, long skip, int limit);

    /**
     * Count distinct monthly timeline groups.
     */
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0, deletedAt: null } }",
        "{ $group: { _id: { $dateToString: { format: '%Y-%m', date: '$uploadedAt' } } } }",
        "{ $count: 'total' }"
    })
    TimelineCountResult countGroupedTimelineMonthly(String userId);

    /**
     * Push yearly timeline grouping down to MongoDB engine using aggregation pipeline.
     */
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0, deletedAt: null } }",
        "{ $group: { _id: { $dateToString: { format: '%Y', date: '$uploadedAt' } }, count: { $sum: 1 } } }",
        "{ $sort: { '_id': -1 } }"
    })
    List<TimelineGroupAggregationResult> getGroupedTimelineYearly(String userId);

    /**
     * Paginated yearly timeline grouping using MongoDB aggregation pipeline.
     */
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0, deletedAt: null } }",
        "{ $group: { _id: { $dateToString: { format: '%Y', date: '$uploadedAt' } }, count: { $sum: 1 } } }",
        "{ $sort: { '_id': -1 } }",
        "{ $skip: ?1 }",
        "{ $limit: ?2 }"
    })
    List<TimelineGroupAggregationResult> getGroupedTimelineYearlyPaginated(String userId, long skip, int limit);

    /**
     * Count distinct yearly timeline groups.
     */
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0, deletedAt: null } }",
        "{ $group: { _id: { $dateToString: { format: '%Y', date: '$uploadedAt' } } } }",
        "{ $count: 'total' }"
    })
    TimelineCountResult countGroupedTimelineYearly(String userId);

    /**
     * Group by media type and sum size in bytes using MongoDB aggregation.
     */
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0, deletedAt: null } }",
        "{ $group: { _id: '$type', totalBytes: { $sum: '$sizeBytes' }, count: { $sum: 1 } } }"
    })
    List<StorageTypeAggregationResult> getStorageUsageByType(String userId);
}
