package com.example.Media.application.service.interfaces;

import com.example.Media.infrastructure.persistence.document.MediaDocument;
import com.example.Media.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Media.shared.enums.MediaDomain;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

/**
 * Service contract for all media lifecycle operations.
 *
 * <p>This interface inverts the dependency between the API layer and the concrete
 * service implementation, satisfying the Dependency Inversion Principle (DIP).
 * The controller and any other consumer depend on this abstraction; the concrete
 * {@code MediaServiceImpl} is injected by Spring's IoC container at runtime.</p>
 */
public interface IMediaService {

    /**
     * Upload a file with SHA-256 deduplication support.
     *
     * @param userId     the authenticated user performing the upload
     * @param domain     the business domain this asset belongs to
     * @param entityRefId optional reference to a parent entity (event, profile, etc.)
     * @param file       the multipart file binary
     * @return the created (or deduplicated) {@link MediaDocument}
     */
    MediaDocument uploadMedia(String userId, MediaDomain domain, String entityRefId, MultipartFile file);

    /**
     * Retrieve the binary file resource for streaming/download.
     *
     * @param mediaId the media document identifier
     * @return the loadable {@link Resource}
     */
    Resource getMediaFile(String mediaId);

    /**
     * Get the metadata document for a media asset.
     *
     * @param mediaId the media document identifier
     * @return the active (non-deleted) {@link MediaDocument}
     * @throws RuntimeException if not found or soft-deleted
     */
    MediaDocument getMediaDetails(String mediaId);

    /**
     * Soft-delete a media asset and close its storage ledger entry.
     *
     * @param mediaId the media document identifier
     */
    void deleteMedia(String mediaId);

    /**
     * List active media for a user filtered by domain.
     *
     * @param userId the user identifier
     * @param domain the business domain filter
     * @return list of active media documents in the given domain
     */
    List<MediaDocument> listMediaByUserAndDomain(String userId, MediaDomain domain);

    /**
     * List all active media for a user across every domain.
     *
     * @param userId the user identifier
     * @return list of all active media documents
     */
    List<MediaDocument> listMediaByUser(String userId);

    /**
     * Calculate total active storage consumption for a user (in bytes).
     *
     * @param userId the user identifier
     * @return total bytes of active media
     */
    long getStorageUsageBytes(String userId);

    /**
     * Query the storage usage ledger for a user within a billing period.
     * Used by the Billing microservice for cost calculation.
     *
     * @param userId the user identifier
     * @param start  inclusive start of the billing window
     * @param end    inclusive end of the billing window
     * @return ledger entries falling within the period
     */
    List<StorageUsageLedgerDocument> getStorageLedger(String userId, Instant start, Instant end);
}
