package com.example.Media.application.service.impl;

import com.example.Media.api.dto.response.HealthResponse;
import com.example.Media.api.dto.response.MediaDeleteResponse;
import com.example.Media.api.dto.response.MediaResponse;
import com.example.Media.api.dto.response.StorageUsageLedgerResponse;
import com.example.Media.api.dto.response.StorageUsageResponse;
import com.example.Media.application.service.interfaces.IAsyncStoragePurgeWorker;
import com.example.Media.application.service.interfaces.IMediaService;
import com.example.Media.infrastructure.mapper.MediaMapper;
import com.example.Media.infrastructure.persistence.document.MediaDocument;
import com.example.Media.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Media.infrastructure.persistence.repository.MediaRepository;
import com.example.Media.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import com.example.Media.infrastructure.storage.StorageProvider;
import com.example.Media.shared.enums.MediaDomain;
import com.example.Media.shared.exception.DomainValidationException;
import com.example.Media.shared.exception.MediaProcessingException;
import com.example.Media.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Concrete implementation of {@link IMediaService}.
 *
 * <p>All business logic has been moved verbatim from the former {@code MediaService}
 * class. The only structural changes are:
 * <ul>
 *   <li>This class now {@code implements IMediaService}, satisfying Dependency Inversion.</li>
 *   <li>{@code String domain} parameters are replaced with the type-safe {@link MediaDomain}
 *       enum to align with the updated document and repository contracts.</li>
 *   <li>A new {@link #getStorageLedger} method has been added to decouple the controller
 *       from the {@link StorageUsageLedgerRepository}.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements IMediaService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "video/mp4", "video/webm"
    );

    private final MediaRepository mediaRepository;
    private final StorageUsageLedgerRepository ledgerRepository;
    private final StorageProvider storageProvider;
    private final Tika tika;
    private final MediaMapper mediaMapper;
    private final IAsyncStoragePurgeWorker asyncStoragePurgeWorker;

    @Value("${storage.limit.bytes}")
    private long storageLimitBytes;

    @Value("${sangrah.storage.provider}")
    private String storageProviderName;

    /**
     * Upload a file with deduplication support
     */
    @Override
    public MediaResponse uploadMedia(String userId, MediaDomain domain, String entityRefId, MultipartFile file) {
        String storageKey = null;
        try {
            log.info("📤 [Media Upload] Starting upload for user: {} | domain: {} | file: {}",
                userId, domain, file.getOriginalFilename());

            // Step 1: Calculate SHA-256 checksum (for deduplication) - Stream I/O outside transaction
            String checksum = calculateFileChecksum(file.getInputStream());
            log.info("🔐 [Media Upload] Checksum calculated: {}", checksum);

            // Step 2: Check for duplicates (same file already uploaded) - Read query outside transaction
            Optional<MediaDocument> existingMedia = mediaRepository.findByChecksumSha256AndDeletedAtIsNull(checksum);
            if (existingMedia.isPresent() && existingMedia.get().getUserId().equals(userId)) {
                log.info("♻️  [Media Upload] Duplicate detected - reusing existing media: {}", existingMedia.get().getId());
                return mediaMapper.toResponse(existingMedia.get());  // Return existing instead of uploading again
            }

            // Step 3: Determine MIME type - Stream I/O outside transaction
            String mimeType;
            try (InputStream is = file.getInputStream()) {
                mimeType = tika.detect(is);
            }
            log.debug("📄 [Media Upload] MIME type detected: {}", mimeType);

            if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
                throw new DomainValidationException("Unsupported or malicious file type detected: " + mimeType);
            }

            // Step 4: Save file to storage provider - Disk/Network I/O outside transaction
            storageKey = storageProvider.save(file);
            log.info("💾 [Media Upload] File saved with key: {}", storageKey);

            // Step 5 & 6: Delegate pure database operations to transactional boundary
            return persistMediaRecords(userId, domain, entityRefId, file, checksum, mimeType, storageKey);

        } catch (IOException e) {
            log.error("❌ [Media Upload] Failed to upload file: {}", e.getMessage(), e);
            throw new MediaProcessingException("Failed to upload file due to an internal storage error.", e);
        } catch (Exception e) {
            if (storageKey != null) {
                try {
                    storageProvider.delete(storageKey);
                    log.warn("🧹 [Media Upload] Cleaned up orphaned physical file after failure: {}", storageKey);
                } catch (IOException cleanupEx) {
                    log.error("❌ [Media Upload] Failed to clean up orphaned storage file {}: {}", storageKey, cleanupEx.getMessage());
                }
            }
            throw e;
        }
    }

    /**
     * Persist metadata and ledger entry within a scoped database transaction.
     */
    @Transactional
    protected MediaResponse persistMediaRecords(
            String userId,
            MediaDomain domain,
            String entityRefId,
            MultipartFile file,
            String checksum,
            String mimeType,
            String storageKey) {
        Instant now = Instant.now();

        // Step 5: Create MediaDocument
        MediaDocument mediaDoc = MediaDocument.builder()
                .userId(userId)
                .domain(domain)
                .entityRefId(entityRefId)
                .fileName(file.getOriginalFilename())
                .originalFileName(file.getOriginalFilename())
                .mimeType(mimeType)
                .sizeBytes(file.getSize())
                .storageKey(storageKey)
                .storageProvider(storageProviderName)
                .checksumSha256(checksum)
                .contentType(mimeType)
                .uploadedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        mediaRepository.save(mediaDoc);
        log.info("✅ [Media Upload] MediaDocument created: {}", mediaDoc.getId());

        // Step 6: Create StorageUsageLedger entry for billing
        StorageUsageLedgerDocument ledgerEntry = StorageUsageLedgerDocument.builder()
                .userId(userId)
                .domain(domain)
                .domainRefId(mediaDoc.getId())
                .sizeBytes(file.getSize())
                .startAt(now)
                .sourceService("MediaService")
                .createdAt(now)
                .build();

        ledgerRepository.save(ledgerEntry);
        log.info("📊 [Media Upload] Ledger entry created for billing tracking");

        log.info("✅ [Media Upload] Upload completed successfully");
        return mediaMapper.toResponse(mediaDoc);
    }

    /**
     * Retrieve a file by media ID
     */
    @Override
    public Resource getMediaFile(String mediaId) {
        try {
            log.info("📥 [Media Download] Fetching file for mediaId: {}", mediaId);

            MediaDocument mediaDoc = mediaRepository.findByIdAndDeletedAtIsNull(mediaId)
                    .orElseThrow(() -> {
                        log.error("❌ [Media Download] Media not found or deleted: {}", mediaId);
                        return new ResourceNotFoundException("MediaDocument", "id", mediaId);
                    });

            log.info("📄 [Media Download] Found media: {} | StorageKey: {}", mediaId, mediaDoc.getStorageKey());
            return storageProvider.load(mediaDoc.getStorageKey());

        } catch (IOException e) {
            log.error("❌ [Media Download] Failed to load file: {}", e.getMessage(), e);
            throw new MediaProcessingException("Failed to load file due to an internal storage error.", e);
        }
    }

    /**
     * Get media details by ID
     */
    @Override
    public MediaResponse getMediaDetails(String mediaId) {
        log.info("📋 [Media Details] Fetching details for mediaId: {}", mediaId);

        return mediaMapper.toResponse(mediaRepository.findByIdAndDeletedAtIsNull(mediaId)
                .orElseThrow(() -> {
                    log.error("❌ [Media Details] Media not found: {}", mediaId);
                    return new ResourceNotFoundException("MediaDocument", "id", mediaId);
                }));
    }

    /**
     * Soft delete a media file
     */
    @Override
    @Transactional
    public MediaDeleteResponse deleteMedia(String mediaId) {
        log.info("🗑️  [Media Delete] Soft deleting mediaId: {}", mediaId);

        MediaDocument mediaDoc = mediaRepository.findByIdAndDeletedAtIsNull(mediaId)
                .orElseThrow(() -> {
                    log.error("❌ [Media Delete] Media not found or already deleted: {}", mediaId);
                    return new ResourceNotFoundException("MediaDocument", "id", mediaId);
                });

        // Mark as deleted
        mediaDoc.setDeletedAt(Instant.now());
        mediaDoc.setUpdatedAt(Instant.now());
        mediaRepository.save(mediaDoc);
        log.info("✅ [Media Delete] Media marked as deleted: {}", mediaId);

        // Update ledger entry to mark end of storage usage
        List<StorageUsageLedgerDocument> ledgerEntries = ledgerRepository.findByDomainRefId(mediaId);
        List<StorageUsageLedgerDocument> entriesToUpdate = new ArrayList<>();
        Instant endTimestamp = Instant.now();
        for (StorageUsageLedgerDocument entry : ledgerEntries) {
            if (entry.isActive()) {
                entry.setEndAt(endTimestamp);
                entriesToUpdate.add(entry);
                log.info("📊 [Media Delete] Ledger entry closed: byte-days = {}", entry.getBytesDays());
            }
        }

        if (!entriesToUpdate.isEmpty()) {
            ledgerRepository.saveAll(entriesToUpdate);
        }

        // Purge physical file from storage provider asynchronously to prevent orphaned binaries
        // without blocking transactional threads or network execution
        if (mediaDoc.getStorageKey() != null && !mediaDoc.getStorageKey().isBlank()) {
            asyncStoragePurgeWorker.purgeStorageKeyAsync(mediaDoc.getStorageKey());
        }

        log.info("✅ [Media Delete] Deletion completed");
        return new MediaDeleteResponse("success", "Media deleted");
    }

    /**
     * Bulk soft-delete media files
     */
    @Override
    @Transactional
    public MediaDeleteResponse deleteMediaBulk(List<String> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            log.warn("⚠️  [Media Bulk Delete] Received empty or null mediaIds list");
            return new MediaDeleteResponse("success", "No media IDs provided");
        }

        log.info("🗑️  [Media Bulk Delete] Bulk soft deleting {} media items", mediaIds.size());

        List<MediaDocument> mediaDocs = mediaRepository.findAllById(mediaIds);
        Instant now = Instant.now();
        List<MediaDocument> docsToUpdate = new ArrayList<>();
        List<String> storageKeysToPurge = new ArrayList<>();

        for (MediaDocument doc : mediaDocs) {
            if (doc.getDeletedAt() == null) {
                doc.setDeletedAt(now);
                doc.setUpdatedAt(now);
                docsToUpdate.add(doc);
                if (doc.getStorageKey() != null && !doc.getStorageKey().isBlank()) {
                    storageKeysToPurge.add(doc.getStorageKey());
                }
            }
        }

        if (!docsToUpdate.isEmpty()) {
            mediaRepository.saveAll(docsToUpdate);
            log.info("✅ [Media Bulk Delete] {} media documents marked as deleted", docsToUpdate.size());
        }

        // Close storage usage ledgers for all deleted media
        List<StorageUsageLedgerDocument> ledgerEntries = ledgerRepository.findByDomainRefIdIn(mediaIds);
        List<StorageUsageLedgerDocument> entriesToUpdate = new ArrayList<>();
        for (StorageUsageLedgerDocument entry : ledgerEntries) {
            if (entry.isActive()) {
                entry.setEndAt(now);
                entriesToUpdate.add(entry);
                log.info("📊 [Media Bulk Delete] Ledger entry closed: byte-days = {}", entry.getBytesDays());
            }
        }

        if (!entriesToUpdate.isEmpty()) {
            ledgerRepository.saveAll(entriesToUpdate);
            log.info("📊 [Media Bulk Delete] {} ledger entries saved", entriesToUpdate.size());
        }

        // Purge physical files asynchronously via background worker to eliminate N+1 I/O loops,
        // prevent HTTP worker thread starvation, and avoid holding database transactions open during I/O
        if (!storageKeysToPurge.isEmpty()) {
            asyncStoragePurgeWorker.purgeStorageKeysAsync(storageKeysToPurge);
        }

        log.info("✅ [Media Bulk Delete] Bulk deletion completed for {} items", docsToUpdate.size());
        return new MediaDeleteResponse("success", "Bulk media deleted: " + docsToUpdate.size() + " items");
    }

    /**
     * List media files for a user in a domain
     */
    @Override
    public List<MediaResponse> listMediaByUserAndDomain(String userId, MediaDomain domain) {
        log.debug("📋 [Media List] Listing media for user: {} | domain: {}", userId, domain);

        List<MediaResponse> media = mediaRepository.findByUserIdAndDomainAndDeletedAtIsNull(userId, domain)
                .stream().map(mediaMapper::toResponse).toList();
        log.info("📊 [Media List] Found {} media files", media.size());

        return media;
    }

    /**
     * List all media for a user across all domains
     */
    @Override
    public List<MediaResponse> listMediaByUser(String userId) {
        log.debug("📋 [Media List] Listing all media for user: {}", userId);

        List<MediaResponse> media = mediaRepository.findByUserIdAndDeletedAtIsNull(userId)
                .stream().map(mediaMapper::toResponse).toList();
        log.info("📊 [Media List] Found {} total media files across all domains", media.size());

        return media;
    }

    /**
     * Get storage usage stats for a user
     */
    @Override
    public long getStorageUsageBytes(String userId) {
        Long totalBytes = mediaRepository.sumSizeBytesByUserIdAndDeletedAtIsNull(userId);
        long usage = totalBytes != null ? totalBytes : 0L;
        log.debug("📊 [Storage Usage] User {} is using {} bytes", userId, usage);
        return usage;
    }

    /**
     * Get full storage usage summary (bytes used, limit, and percentage) for a user.
     */
    @Override
    public StorageUsageResponse getStorageUsage(String userId) {
        long usedBytes = getStorageUsageBytes(userId);
        return new StorageUsageResponse(userId, usedBytes, storageLimitBytes,
                storageLimitBytes > 0 ? (usedBytes * 100.0) / storageLimitBytes : 0.0);
    }

    /**
     * Query the storage usage ledger for a user within a billing period.
     */
    @Override
    public List<StorageUsageLedgerResponse> getStorageLedger(String userId, Instant start, Instant end) {
        log.info("📊 [Storage Ledger] Querying ledger | user: {} | period: {} to {}", userId, start, end);

        List<StorageUsageLedgerResponse> ledgerEntries = ledgerRepository
                .findByUserIdAndStartAtBetween(userId, start, end)
                .stream().map(mediaMapper::toLedgerResponse).toList();
        log.info("📋 [Storage Ledger] Found {} ledger entries", ledgerEntries.size());

        return ledgerEntries;
    }

    /**
     * Calculate SHA-256 checksum of a file
     */
    private String calculateFileChecksum(InputStream inputStream) throws IOException {
        try (InputStream is = inputStream) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            return HexFormat.of().formatHex(digest.digest());

        } catch (NoSuchAlgorithmException e) {
            log.error("❌ SHA-256 not supported: {}", e.getMessage());
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }

    @Override
    public HealthResponse getHealth() {
        return new HealthResponse("UP");
    }
}
