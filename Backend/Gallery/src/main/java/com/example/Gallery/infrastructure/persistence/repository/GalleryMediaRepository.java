package com.example.Gallery.infrastructure.persistence.repository;

import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
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
}
