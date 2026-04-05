package com.example.Media.application.service;

import com.example.Media.infrastructure.persistence.document.MediaDocument;
import com.example.Media.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Media.infrastructure.persistence.repository.MediaRepository;
import com.example.Media.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import com.example.Media.infrastructure.storage.StorageProvider;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final StorageUsageLedgerRepository ledgerRepository;
    private final StorageProvider storageProvider;
    private final Tika tika;

    /**
     * Upload a file with deduplication support
     */
    public MediaDocument uploadMedia(String userId, String domain, String entityRefId, MultipartFile file) {
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
    public Resource getMediaFile(String mediaId) {
        try {
            log.info("📥 [Media Download] Fetching file for mediaId: {}", mediaId);

            MediaDocument mediaDoc = mediaRepository.findByIdAndDeletedAtIsNull(mediaId)
                    .orElseThrow(() -> {
                        log.error("❌ [Media Download] Media not found or deleted: {}", mediaId);
                        return new RuntimeException("Media not found or deleted: " + mediaId);
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
    public MediaDocument getMediaDetails(String mediaId) {
        log.info("📋 [Media Details] Fetching details for mediaId: {}", mediaId);

        return mediaRepository.findByIdAndDeletedAtIsNull(mediaId)
                .orElseThrow(() -> {
                    log.error("❌ [Media Details] Media not found: {}", mediaId);
                    return new RuntimeException("Media not found: " + mediaId);
                });
    }

    /**
     * Soft delete a media file
     */
    public void deleteMedia(String mediaId) {
        try {
            log.info("🗑️  [Media Delete] Soft deleting mediaId: {}", mediaId);

            MediaDocument mediaDoc = mediaRepository.findByIdAndDeletedAtIsNull(mediaId)
                    .orElseThrow(() -> {
                        log.error("❌ [Media Delete] Media not found or already deleted: {}", mediaId);
                        return new RuntimeException("Media not found or already deleted: " + mediaId);
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
    public List<MediaDocument> listMediaByUserAndDomain(String userId, String domain) {
        log.debug("📋 [Media List] Listing media for user: {} | domain: {}", userId, domain);

        List<MediaDocument> media = mediaRepository.findByUserIdAndDomainAndDeletedAtIsNull(userId, domain);
        log.info("📊 [Media List] Found {} media files", media.size());

        return media;
    }

    /**
     * List all media for a user across all domains
     */
    public List<MediaDocument> listMediaByUser(String userId) {
        log.debug("📋 [Media List] Listing all media for user: {}", userId);

        List<MediaDocument> media = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        log.info("📊 [Media List] Found {} total media files across all domains", media.size());

        return media;
    }

    /**
     * Get storage usage stats for a user
     */
    public long getStorageUsageBytes(String userId) {
        List<MediaDocument> userMedia = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        long totalBytes = userMedia.stream().mapToLong(m -> m.getSizeBytes() != null ? m.getSizeBytes() : 0L).sum();
        log.debug("📊 [Storage Usage] User {} is using {} bytes", userId, totalBytes);
        return totalBytes;
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
