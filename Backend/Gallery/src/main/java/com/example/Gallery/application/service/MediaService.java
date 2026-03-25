package com.example.Gallery.application.service;

import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.StorageUsageResponse;
import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
import com.example.Gallery.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Gallery.infrastructure.persistence.repository.GalleryMediaRepository;
import com.example.Gallery.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import com.example.Gallery.infrastructure.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final GalleryMediaRepository mediaRepository;
    private final StorageUsageLedgerRepository storageUsageLedgerRepository;
    private final StorageProvider storageProvider;

    /**
     * Upload media file with deduplication and ledger tracking
     */
    @Transactional
    public MediaItemResponse uploadMedia(String userId, MultipartFile file) throws IOException {
        log.info("📤 Uploading media for user: {}, file: {}", userId, file.getOriginalFilename());

        // 1. Calculate SHA-256 hash for deduplication
        String sha256Hash = calculateSha256Hash(file.getBytes());
        log.debug("Hash computed: {}", sha256Hash);

        // 2. Check for duplicates (user already uploaded this file?)
        Optional<GalleryMediaDocument> existingMedia =
                mediaRepository.findByUserIdAndChecksumSha256(userId, sha256Hash);

        if (existingMedia.isPresent() && !existingMedia.get().isDeleted()) {
            log.info("✅ Duplicate detected! Returning existing media: {}", existingMedia.get().getId());
            return mapToResponse(existingMedia.get());
        }

        // 3. Store file via StorageProvider
        String storageKey;
        try {
            storageKey = storageProvider.save(file);
            log.info("💾 File saved to storage: {}", storageKey);
        } catch (Exception e) {
            log.error("❌ Storage save failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save file: " + e.getMessage());
        }

        // 4. Create MediaDocument
        String type = file.getContentType() != null && file.getContentType().startsWith("video/") ? "VIDEO" : "IMAGE";
        GalleryMediaDocument media = GalleryMediaDocument.builder()
                .userId(userId)
                .ownerUserId(userId)
                .originalFileName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .sizeBytes(file.getSize())
                .storageKey(storageKey)
                .storageProvider("LOCAL")
                .checksumSha256(sha256Hash)
                .type(type)
                .visibility("PRIVATE")
                .uploadedAt(Instant.now())
                .deletedAt(null)  // Not deleted
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .metadata(extractMetadata(file))
                .build();

        GalleryMediaDocument savedMedia = mediaRepository.save(media);
        log.info("✅ MediaDocument saved: {}", savedMedia.getId());

        // 5. CREATE STORAGE USAGE LEDGER ENTRY (CRITICAL FOR BILLING!)
        StorageUsageLedgerDocument ledgerEntry = StorageUsageLedgerDocument.builder()
                .userId(userId)
                .domain("GALLERY")
                .domainRefId(savedMedia.getId())
                .sizeBytes(file.getSize())
                .startAt(Instant.now())
                .endAt(null)  // Not deleted yet - null means active
                .sourceService("Gallery")
                .createdAt(Instant.now())
                .build();

        StorageUsageLedgerDocument savedLedger = storageUsageLedgerRepository.save(ledgerEntry);
        log.info("🔍 StorageUsageLedger entry created: {} (CRITICAL FOR BILLING)", savedLedger.getId());

        return mapToResponse(savedMedia);
    }

    /**
     * Get user's current storage usage (informational, NOT a quota limit)
     */
    public StorageUsageResponse getStorageUsage(String userId) {
        log.info("📊 Fetching storage usage for user: {}", userId);

        // Get only active (non-deleted) files
        List<GalleryMediaDocument> activeMedia = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        log.info("Found {} active files for user {}", activeMedia.size(), userId);

        // Map to response DTOs
        List<MediaItemResponse> mediaResponses = activeMedia.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        // Calculate storage stats
        StorageUsageResponse response = new StorageUsageResponse();
        response.calculateUsage(mediaResponses);

        log.info("📊 Storage stats: {} bytes, {} files", response.getTotalBytesUsed(), response.getFileCount());
        return response;
    }

    /**
     * Get single media item by ID
     */
    public MediaItemResponse getMedia(String userId, String mediaId) {
        log.info("🔍 Fetching media {} for user {}", mediaId, userId);

        GalleryMediaDocument media = mediaRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new RuntimeException("Media not found or access denied"));

        return mapToResponse(media);
    }

    /**
     * Get media file for download/streaming
     */
    public Resource getMediaFile(String userId, String mediaId) {
        log.info("📥 Fetching media file {} for user {}", mediaId, userId);

        GalleryMediaDocument media = mediaRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new RuntimeException("Media not found or access denied"));

        if (media.isDeleted()) {
            throw new RuntimeException("Media has been deleted");
        }

        // Load file from storage
        Resource resource = storageProvider.load(media.getStorageKey());
        log.info("✅ File loaded: {}", media.getStorageKey());
        return resource;
    }

    /**
     * Delete media (soft delete - mark deletedAt, don't actually remove file)
     */
    @Transactional
    public void deleteMedia(String userId, String mediaId) {
        log.info("🗑️ Deleting media {} for user {}", mediaId, userId);

        GalleryMediaDocument media = mediaRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new RuntimeException("Media not found or access denied"));

        if (media.isDeleted()) {
            log.warn("⚠️ Media already deleted: {}", mediaId);
            return;
        }

        // 1. Mark media as deleted (soft delete)
        media.setDeletedAt(Instant.now());
        media.setUpdatedAt(Instant.now());
        mediaRepository.save(media);
        log.info("✅ Media marked as deleted: {}", mediaId);

        // 2. MARK STORAGE LEDGER ENTRY AS DELETED (CRITICAL FOR BILLING!)
        List<StorageUsageLedgerDocument> ledgerEntries = storageUsageLedgerRepository.findByDomainRefId(mediaId);
        for (StorageUsageLedgerDocument entry : ledgerEntries) {
            entry.setEndAt(Instant.now());
            storageUsageLedgerRepository.save(entry);
            log.info("🔍 StorageUsageLedger entry marked deleted: {} (CRITICAL FOR BILLING)", entry.getId());
        }

        log.info("✅ Soft delete complete for media: {}", mediaId);
    }

    /**
     * List all NON-DELETED media for a user (for timeline/gallery view)
     */
    public List<MediaItemResponse> listUserMedia(String userId) {
        log.info("📋 Listing media for user: {}", userId);

        List<GalleryMediaDocument> media = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        log.info("Found {} active media items", media.size());

        return media.stream()
                .map(this::mapToResponse)
                .sorted(Comparator.comparing(MediaItemResponse::getUploadedAt).reversed())  // Newest first
                .collect(Collectors.toList());
    }

    /**
     * Get storage ledger entries for a date range (for Billing service)
     * This is exposed via REST API so other services can query Gallery's ledger data
     * without direct database access (microservice principle)
     */
    public List<com.example.Gallery.api.dto.StorageUsageLedgerDTO> getLedgerEntriesBetweenDates(
            Instant startDate, Instant endDate) {
        log.info("📊 Querying ledger entries from {} to {}", startDate, endDate);

        // Query storage ledger for entries in this date range
        List<StorageUsageLedgerDocument> ledgerEntries =
            storageUsageLedgerRepository.findByStartAtBetween(startDate, endDate);

        log.info("📋 Found {} ledger entries in date range", ledgerEntries.size());

        // Convert documents to DTOs for REST API response
        return ledgerEntries.stream()
                .map(this::mapLedgerToDTO)
                .collect(Collectors.toList());
    }

    // ============ HELPER METHODS ============

    /**
     * Calculate SHA-256 hash for file deduplication
     */
    private String calculateSha256Hash(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(fileBytes);
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Extract metadata from uploaded file (width, height, duration, EXIF)
     * For v1.0: just basic info, can expand in v2.0 with image processing libraries
     */
    private Map<String, Object> extractMetadata(MultipartFile file) {
        Map<String, Object> metadata = new HashMap<>();
        // TODO: In v2.0, use image/video processing libraries to extract:
        // - Image dimensions (width, height)
        // - Video duration
        // - EXIF data (camera, lens, ISO, etc.)
        // For now, just store basic info
        metadata.put("filename", file.getOriginalFilename());
        metadata.put("contentType", file.getContentType());
        return metadata;
    }

    /**
     * Map GalleryMediaDocument to response DTO
     */
    private MediaItemResponse mapToResponse(GalleryMediaDocument media) {
        return MediaItemResponse.builder()
                .id(media.getId())
                .originalFileName(media.getOriginalFileName())
                .mimeType(media.getMimeType())
                .sizeBytes(media.getSizeBytes())
                .type(media.getType())
                .checksumSha256(media.getChecksumSha256())
                .metadata(media.getMetadata())
                .uploadedAt(media.getUploadedAt())
                .deletedAt(media.getDeletedAt())
                .visibilityStatus(media.getVisibility())
                .build();
    }

    /**
     * Map StorageUsageLedgerDocument to DTO for REST API
     * Used when exposing ledger data to other services (Billing)
     */
    private com.example.Gallery.api.dto.StorageUsageLedgerDTO mapLedgerToDTO(StorageUsageLedgerDocument ledger) {
        return com.example.Gallery.api.dto.StorageUsageLedgerDTO.builder()
                .id(ledger.getId())
                .userId(ledger.getUserId())
                .domain(ledger.getDomain())
                .domainRefId(ledger.getDomainRefId())
                .sizeBytes(ledger.getSizeBytes())
                .startAt(ledger.getStartAt())
                .endAt(ledger.getEndAt())
                .sourceService(ledger.getSourceService())
                .createdAt(ledger.getCreatedAt())
                .build();
    }
}
