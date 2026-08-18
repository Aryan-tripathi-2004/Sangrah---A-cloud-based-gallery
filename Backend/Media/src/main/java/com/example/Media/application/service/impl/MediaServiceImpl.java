package com.example.Media.application.service.impl;

import com.example.Media.application.service.interfaces.IMediaService;
import com.example.Media.infrastructure.persistence.document.MediaDocument;
import com.example.Media.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Media.infrastructure.persistence.repository.MediaRepository;
import com.example.Media.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import com.example.Media.infrastructure.storage.StorageProvider;
import com.example.Media.shared.enums.MediaDomain;
import com.example.Media.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

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

    private final MediaRepository mediaRepository;
    private final StorageUsageLedgerRepository ledgerRepository;
    private final StorageProvider storageProvider;
    private final Tika tika;

    /**
     * Upload a file with deduplication support
     */
    @Override
    public MediaDocument uploadMedia(String userId, MediaDomain domain, String entityRefId, MultipartFile file) {
        try {
            log.info("📤 [Media Upload] Starting upload for user: {} | domain: {} | file: {}",
                userId, domain, file.getOriginalFilename());

            // Step 1: Calculate SHA-256 checksum (for deduplication)
            String checksum = calculateFileChecksum(file.getInputStream());
            log.info("🔐 [Media Upload] Checksum calculated: {}", checksum);

            // Step 2: Check for duplicates (same file already uploaded)
            Optional<MediaDocument> existingMedia = mediaRepository.findByChecksumSha256AndDeletedAtIsNull(checksum);
            if (existingMedia.isPresent() && existingMedia.get().getUserId().equals(userId)) {
                log.info("♻️  [Media Upload] Duplicate detected - reusing existing media: {}", existingMedia.get().getId());
                return existingMedia.get();  // Return existing instead of uploading again
            }

            // Step 3: Determine MIME type
            String mimeType = tika.detect(file.getInputStream());
            log.debug("📄 [Media Upload] MIME type detected: {}", mimeType);

            // Step 4: Save file to storage provider
            String storageKey = storageProvider.save(file);
            log.info("💾 [Media Upload] File saved with key: {}", storageKey);

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
                    .storageProvider("LOCAL")
                    .checksumSha256(checksum)
                    .contentType(mimeType)
                    .uploadedAt(Instant.now())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            mediaRepository.save(mediaDoc);
            log.info("✅ [Media Upload] MediaDocument created: {}", mediaDoc.getId());

            // Step 6: Create StorageUsageLedger entry for billing
            StorageUsageLedgerDocument ledgerEntry = StorageUsageLedgerDocument.builder()
                    .userId(userId)
                    .domain(domain)
                    .domainRefId(mediaDoc.getId())
                    .sizeBytes(file.getSize())
                    .startAt(Instant.now())
                    .sourceService("MediaService")
                    .createdAt(Instant.now())
                    .build();

            ledgerRepository.save(ledgerEntry);
            log.info("📊 [Media Upload] Ledger entry created for billing tracking");

            log.info("✅ [Media Upload] Upload completed successfully");
            return mediaDoc;

        } catch (IOException e) {
            log.error("❌ [Media Upload] Failed to upload file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
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
            throw new RuntimeException("Failed to load file: " + e.getMessage(), e);
        }
    }

    /**
     * Get media details by ID
     */
    @Override
    public MediaDocument getMediaDetails(String mediaId) {
        log.info("📋 [Media Details] Fetching details for mediaId: {}", mediaId);

        return mediaRepository.findByIdAndDeletedAtIsNull(mediaId)
                .orElseThrow(() -> {
                    log.error("❌ [Media Details] Media not found: {}", mediaId);
                    return new ResourceNotFoundException("MediaDocument", "id", mediaId);
                });
    }

    /**
     * Soft delete a media file
     */
    @Override
    public void deleteMedia(String mediaId) {
        try {
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
            for (StorageUsageLedgerDocument entry : ledgerEntries) {
                if (entry.isActive()) {
                    entry.setEndAt(Instant.now());
                    ledgerRepository.save(entry);
                    log.info("📊 [Media Delete] Ledger entry closed: byte-days = {}", entry.getBytesDays());
                }
            }

            log.info("✅ [Media Delete] Deletion completed");

        } catch (Exception e) {
            log.error("❌ [Media Delete] Failed to delete media: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete media: " + e.getMessage(), e);
        }
    }

    /**
     * List media files for a user in a domain
     */
    @Override
    public List<MediaDocument> listMediaByUserAndDomain(String userId, MediaDomain domain) {
        log.debug("📋 [Media List] Listing media for user: {} | domain: {}", userId, domain);

        List<MediaDocument> media = mediaRepository.findByUserIdAndDomainAndDeletedAtIsNull(userId, domain);
        log.info("📊 [Media List] Found {} media files", media.size());

        return media;
    }

    /**
     * List all media for a user across all domains
     */
    @Override
    public List<MediaDocument> listMediaByUser(String userId) {
        log.debug("📋 [Media List] Listing all media for user: {}", userId);

        List<MediaDocument> media = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        log.info("📊 [Media List] Found {} total media files across all domains", media.size());

        return media;
    }

    /**
     * Get storage usage stats for a user
     */
    @Override
    public long getStorageUsageBytes(String userId) {
        List<MediaDocument> userMedia = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        long totalBytes = userMedia.stream().mapToLong(m -> m.getSizeBytes() != null ? m.getSizeBytes() : 0L).sum();
        log.debug("📊 [Storage Usage] User {} is using {} bytes", userId, totalBytes);
        return totalBytes;
    }

    /**
     * Query the storage usage ledger for a user within a billing period.
     */
    @Override
    public List<StorageUsageLedgerDocument> getStorageLedger(String userId, Instant start, Instant end) {
        log.info("📊 [Storage Ledger] Querying ledger | user: {} | period: {} to {}", userId, start, end);

        List<StorageUsageLedgerDocument> ledgerEntries = ledgerRepository.findByUserIdAndStartAtBetween(userId, start, end);
        log.info("📋 [Storage Ledger] Found {} ledger entries", ledgerEntries.size());

        return ledgerEntries;
    }

    /**
     * Calculate SHA-256 checksum of a file
     */
    private String calculateFileChecksum(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            return HexFormat.of().formatHex(digest.digest());

        } catch (Exception e) {
            log.error("❌ Failed to calculate checksum: {}", e.getMessage());
            throw new IOException("Failed to calculate checksum: " + e.getMessage(), e);
        }
    }
}
