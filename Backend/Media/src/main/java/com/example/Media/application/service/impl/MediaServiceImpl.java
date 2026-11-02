package com.example.Media.application.service.impl;

import com.example.Media.api.dto.response.HealthResponse;
import com.example.Media.api.dto.response.MediaDeleteResponse;
import com.example.Media.api.dto.response.MediaResponse;
import com.example.Media.api.dto.response.StorageUsageLedgerResponse;
import com.example.Media.api.dto.response.StorageUsageResponse;
import com.example.Media.application.service.helper.MediaPersistenceHelper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * Concrete implementation of {@link IMediaService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements IMediaService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "video/mp4", "video/webm"
    );

    private final Tika tika;
    private final MediaRepository mediaRepository;
    private final StorageUsageLedgerRepository ledgerRepository;
    private final StorageProvider storageProvider;
    private final MediaMapper mediaMapper;
    private final IAsyncStoragePurgeWorker asyncStoragePurgeWorker;
    private final MediaPersistenceHelper mediaPersistenceHelper;

    @Value("${media.storage.limit.bytes:10737418240}")
    private long storageLimitBytes;

    @Value("${media.upload.max-size}")
    private long maxFileSize;

    @Value("${sangrah.storage.provider:LOCAL}")
    private String storageProviderName;

    /**
     * Upload a file with SHA-256 deduplication and per-domain routing.
     */
    @Override
    public MediaResponse uploadMedia(String userId, MediaDomain domain, String entityRefId, MultipartFile file) {
        log.info("📤 [Media Upload] Request | user: {} | domain: {} | entity: {} | file: {} ({} bytes)",
                userId, domain, entityRefId, file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            throw new DomainValidationException("File cannot be empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new DomainValidationException("File size exceeds limit: " + file.getSize() + " > " + maxFileSize);
        }

        String mimeType;
        try {
            mimeType = tika.detect(file.getInputStream());
        } catch (Exception e) {
            log.error("❌ Failed to detect MIME type: {}", e.getMessage());
            throw new DomainValidationException("Unable to determine MIME type", e);
        }

        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            log.warn("⚠️  Disallowed MIME type: {}", mimeType);
            throw new DomainValidationException("Unsupported MIME type: " + mimeType);
        }

        // Check user quota
        long currentUsage = getStorageUsageBytes(userId);
        if (currentUsage + file.getSize() > storageLimitBytes) {
            log.warn("❌ Quota exceeded for user {}: current={} limit={}", userId, currentUsage, storageLimitBytes);
            throw new DomainValidationException("Storage quota exceeded");
        }

        // SHA-256 deduplication check
        String checksum;
        try {
            checksum = calculateFileChecksum(file.getInputStream());
        } catch (IOException e) {
            log.error("❌ Failed to calculate checksum: {}", e.getMessage());
            throw new MediaProcessingException("Failed to calculate checksum", e);
        }

        Optional<MediaDocument> existingMedia = mediaRepository.findByChecksumSha256AndDeletedAtIsNull(checksum);
        if (existingMedia.isPresent() && existingMedia.get().getUserId().equals(userId)) {
            log.info("♻️  [Media Upload] Dedup HIT: existing ID={}", existingMedia.get().getId());
            return mediaMapper.toResponse(existingMedia.get());
        }

        // Upload to storage provider
        String storageKey;
        try {
            storageKey = storageProvider.save(file);
            log.info("☁️  [Media Upload] Binary saved to storage provider: key={}", storageKey);
        } catch (Exception e) {
            log.error("❌ Storage provider failure: {}", e.getMessage());
            throw new MediaProcessingException("Failed to save binary to storage provider", e);
        }

        // Delegate transactional record creation to dedicated helper bean (fixes Spring AOP self-invocation bypass)
        try {
            return mediaPersistenceHelper.persistMediaRecords(
                    userId, domain, entityRefId, file, checksum, mimeType, storageKey, storageProviderName
            );
        } catch (Exception e) {
            log.error("❌ Metadata persistence failed, cleaning up orphaned storage key {}: {}", storageKey, e.getMessage());
            try {
                storageProvider.delete(storageKey);
            } catch (Exception cleanupEx) {
                log.error("CRITICAL: Failed to clean up orphaned binary after DB failure: key={}", storageKey, cleanupEx);
            }
            throw e;
        }
    }

    /**
     * Retrieve a media file stream as a Spring {@link Resource}.
     */
    @Override
    public Resource getMediaFile(String mediaId) {
        log.info("📥 [Media Download] ID: {}", mediaId);

        MediaDocument mediaDoc = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + mediaId));

        if (mediaDoc.isDeleted()) {
            log.warn("⚠️  Media is deleted: {}", mediaId);
            throw new ResourceNotFoundException("Media has been deleted: " + mediaId);
        }

        try {
            return storageProvider.load(mediaDoc.getStorageKey());
        } catch (IOException e) {
            log.error("❌ Failed to load file binary for storageKey {}: {}", mediaDoc.getStorageKey(), e.getMessage());
            throw new MediaProcessingException("Failed to load media file", e);
        }
    }

    /**
     * Retrieve media metadata by ID.
     */
    @Override
    public MediaResponse getMediaById(String mediaId) {
        log.info("🔍 [Media Metadata] ID: {}", mediaId);

        MediaDocument mediaDoc = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + mediaId));

        if (mediaDoc.isDeleted()) {
            throw new ResourceNotFoundException("Media has been deleted: " + mediaId);
        }

        return mediaMapper.toResponse(mediaDoc);
    }

    /**
     * Soft-delete a single media file (internal/system overload)
     */
    @Override
    public MediaDeleteResponse deleteMedia(String mediaId) {
        return deleteMedia(mediaId, "SYSTEM", "ADMIN");
    }

    /**
     * Soft-delete a single media file with BOLA ownership verification
     */
    @Override
    @Transactional
    public MediaDeleteResponse deleteMedia(String mediaId, String userId, String role) {
        log.info("🗑️  [Media Delete] ID: {} | caller: {} | role: {}", mediaId, userId, role);

        MediaDocument mediaDoc = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + mediaId));

        if (mediaDoc.isDeleted()) {
            log.warn("⚠️  Media already deleted: {}", mediaId);
            return new MediaDeleteResponse("success", "Media already deleted");
        }

        // BOLA defense: Verify calling user owns the media or is an administrator/system
        boolean isAdminOrSystem = "SYSTEM".equalsIgnoreCase(userId) 
                || (role != null && (role.toUpperCase().contains("ADMIN") || role.toUpperCase().contains("SYSTEM")));
        if (!isAdminOrSystem && (userId == null || !mediaDoc.getUserId().equals(userId))) {
            log.warn("🚨 [BOLA Violation] User {} attempted to delete media {} owned by {}", userId, mediaId, mediaDoc.getUserId());
            throw new SecurityException("Access denied: You do not own this media resource");
        }

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
     * Bulk soft-delete media files (internal/system overload)
     */
    @Override
    public MediaDeleteResponse deleteMediaBulk(List<String> mediaIds) {
        return deleteMediaBulk(mediaIds, "SYSTEM", "ADMIN");
    }

    /**
     * Bulk soft-delete media files with BOLA ownership verification
     */
    @Override
    @Transactional
    public MediaDeleteResponse deleteMediaBulk(List<String> mediaIds, String userId, String role) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            log.warn("⚠️  [Media Bulk Delete] Received empty or null mediaIds list");
            return new MediaDeleteResponse("success", "No media IDs provided");
        }

        log.info("🗑️  [Media Bulk Delete] Bulk soft deleting {} media items | caller: {} | role: {}", mediaIds.size(), userId, role);

        boolean isAdminOrSystem = "SYSTEM".equalsIgnoreCase(userId) 
                || (role != null && (role.toUpperCase().contains("ADMIN") || role.toUpperCase().contains("SYSTEM")));
        List<MediaDocument> mediaDocs = mediaRepository.findAllById(mediaIds);

        if (!isAdminOrSystem) {
            for (MediaDocument doc : mediaDocs) {
                if (doc.getUserId() != null && !doc.getUserId().equals(userId)) {
                    log.warn("🚨 [BOLA Violation] User {} attempted to bulk-delete media {} owned by {}", userId, doc.getId(), doc.getUserId());
                    throw new SecurityException("Access denied: You do not own all specified media resources");
                }
            }
        }

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
        }

        List<StorageUsageLedgerDocument> ledgerEntries = ledgerRepository.findByDomainRefIdIn(mediaIds);
        List<StorageUsageLedgerDocument> entriesToUpdate = new ArrayList<>();
        for (StorageUsageLedgerDocument entry : ledgerEntries) {
            if (entry.isActive()) {
                entry.setEndAt(now);
                entriesToUpdate.add(entry);
            }
        }

        if (!entriesToUpdate.isEmpty()) {
            ledgerRepository.saveAll(entriesToUpdate);
        }

        for (String storageKey : storageKeysToPurge) {
            asyncStoragePurgeWorker.purgeStorageKeyAsync(storageKey);
        }

        log.info("✅ [Media Bulk Delete] Successfully soft-deleted {} items", docsToUpdate.size());
        return new MediaDeleteResponse("success", docsToUpdate.size() + " items deleted");
    }

    /**
     * List active media for a user.
     */
    @Override
    public List<MediaResponse> listMediaByUser(String userId) {
        log.info("📋 [Media List] Querying all active media for user: {}", userId);
        return mediaRepository.findByUserIdAndDeletedAtIsNull(userId)
                .stream().map(mediaMapper::toResponse).toList();
    }

    /**
     * List active media for a user partitioned by domain.
     */
    @Override
    public List<MediaResponse> listMediaByUserAndDomain(String userId, MediaDomain domain) {
        log.info("📋 [Media List] Querying media for user: {} | domain: {}", userId, domain);
        return mediaRepository.findByUserIdAndDomainAndDeletedAtIsNull(userId, domain)
                .stream().map(mediaMapper::toResponse).toList();
    }

    /**
     * Aggregated active storage consumption for a user.
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
     * Query paginated storage usage ledger entries across all users in a date range (for batch billing).
     */
    @Override
    public Page<StorageUsageLedgerResponse> getLedgerEntriesBetweenDates(Instant startDate, Instant endDate, int page, int size) {
        log.info("📊 [Storage Ledger] Querying paginated ledger entries from {} to {} (page: {}, size: {})", startDate, endDate, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return ledgerRepository.findByStartAtBetween(startDate, endDate, pageable)
                .map(mediaMapper::toLedgerResponse);
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
    public MediaResponse getAuthorizedMediaDetails(String mediaId, String callerUserId, String role) {
        MediaResponse details = getMediaById(mediaId);

        if (MediaDomain.GALLERY.equals(details.domain())) {
            boolean isAdminOrSystem = "SYSTEM".equalsIgnoreCase(callerUserId)
                    || (role != null && (role.toUpperCase().contains("ADMIN") || role.toUpperCase().contains("SYSTEM")));
            if (!isAdminOrSystem && (callerUserId == null || !details.userId().equals(callerUserId))) {
                log.warn("🚨 [BOLA Block] User {} attempted to access private Gallery media {} owned by {}", callerUserId, mediaId, details.userId());
                throw new SecurityException("Access denied: Gallery media is private to its owner");
            }
        }
        return details;
    }

    @Override
    public boolean shouldStreamBinary(String acceptHeader) {
        if (acceptHeader == null) {
            return false;
        }
        if (acceptHeader.contains("application/json")) {
            return false;
        }
        return acceptHeader.contains("image/")
                || acceptHeader.contains("video/")
                || acceptHeader.contains("application/octet-stream");
    }

    @Override
    public List<MediaResponse> listAuthorizedMedia(String userId, String callerUserId, String role, MediaDomain domain) {
        boolean isAdminOrSystem = "SYSTEM".equalsIgnoreCase(callerUserId)
                || (role != null && (role.toUpperCase().contains("ADMIN") || role.toUpperCase().contains("SYSTEM")));
        if (!isAdminOrSystem && (callerUserId == null || !callerUserId.equals(userId))) {
            throw new SecurityException("Access denied: You can only query your own media list");
        }
        return domain != null ? listMediaByUserAndDomain(userId, domain) : listMediaByUser(userId);
    }

    @Override
    public StorageUsageResponse getAuthorizedStorageUsage(String userId, String headerUserId, String role) {
        String targetUserId = (userId != null && !userId.isBlank()) ? userId : headerUserId;
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new IllegalArgumentException("User ID must be provided");
        }

        if (headerUserId != null && !headerUserId.isBlank() && (role == null || !role.toUpperCase().contains("ADMIN"))) {
            if (!headerUserId.equals(targetUserId)) {
                throw new SecurityException("Access denied: You cannot view storage metrics of another user");
            }
        }
        return getStorageUsage(targetUserId);
    }

    @Override
    public List<StorageUsageLedgerResponse> getAuthorizedStorageLedger(String userId, Instant startDate, Instant endDate, String headerUserId, String role) {
        if (headerUserId != null && (role == null || !role.toUpperCase().contains("ADMIN"))) {
            throw new SecurityException("Access denied: Storage ledger is restricted to internal services and administrators");
        }
        return getStorageLedger(userId, startDate, endDate);
    }

    @Override
    public HealthResponse getHealth() {
        return new HealthResponse("UP");
    }
}
