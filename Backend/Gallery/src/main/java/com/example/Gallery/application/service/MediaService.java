package com.example.Gallery.application.service;

import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.StorageUsageResponse;
import com.example.Gallery.infrastructure.client.MediaServiceClient;
import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
import com.example.Gallery.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Gallery.infrastructure.persistence.repository.GalleryMediaRepository;
import com.example.Gallery.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;

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
    private final MediaServiceClient mediaServiceClient;

    /**
     * Upload media file - delegates to Media Service
     */
    @Transactional
    public MediaItemResponse uploadMedia(String userId, MultipartFile file) throws IOException {
        log.info("📤 Uploading media for user: {} via Media Service, file: {}", userId, file.getOriginalFilename());

        try {
            // Call Media Service to handle actual file upload
            Map<String, Object> mediaResponse = mediaServiceClient.uploadMedia(
                    file,
                    "GALLERY",
                    null,
                    userId
            );
            log.info("✅ File uploaded to Media Service, response: {}", mediaResponse);

            String mediaId = (String) mediaResponse.get("id");
            String storageKey = (String) mediaResponse.get("storageKey");
            String checksum = (String) mediaResponse.get("checksumSha256");

            // Store gallery-specific metadata in local database for fast queries
            String type = file.getContentType() != null && file.getContentType().startsWith("video/") ? "VIDEO" : "IMAGE";
            GalleryMediaDocument galleryMedia = GalleryMediaDocument.builder()
                    .id(mediaId)  // Use same ID from Media Service
                    .userId(userId)
                    .ownerUserId(userId)
                    .originalFileName(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .storageKey(storageKey)
                    .storageProvider("MEDIA_SERVICE")
                    .checksumSha256(checksum)
                    .type(type)
                    .visibility("PRIVATE")
                    .uploadedAt(Instant.now())
                    .deletedAt(null)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .metadata(extractMetadata(file))
                    .build();

            GalleryMediaDocument savedGalleryMedia = mediaRepository.save(galleryMedia);
            log.info("✅ Gallery metadata saved: {}", savedGalleryMedia.getId());

            return mapToResponse(savedGalleryMedia);

        } catch (Exception e) {
            log.error("❌ Upload to Media Service failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Get user's current storage usage
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
     * Get media file for download/streaming - delegates to Media Service
     */
    public Resource getMediaFile(String userId, String mediaId) {
        log.info("📥 Fetching media file {} for user {}", mediaId, userId);

        GalleryMediaDocument media = mediaRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new RuntimeException("Media not found or access denied"));

        if (media.isDeleted()) {
            throw new RuntimeException("Media has been deleted");
        }

        try {
            // Fetch file bytes from Media Service
            byte[] fileBytes = mediaServiceClient.getMediaFile(mediaId, userId);
            log.info("✅ File retrieved from Media Service: {} bytes", fileBytes.length);

            // Return as ByteArrayResource for Spring to stream
            return new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return media.getOriginalFileName();
                }
            };

        } catch (Exception e) {
            log.error("❌ Failed to fetch file from Media Service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load file from Media Service: " + e.getMessage(), e);
        }
    }

    /**
     * Delete media (soft delete) - delegates to Media Service
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

        try {
            // Call Media Service to soft delete
            mediaServiceClient.deleteMedia(mediaId, userId);
            log.info("✅ Media Service deletion confirmed");

            // Mark in local gallery database
            media.setDeletedAt(Instant.now());
            media.setUpdatedAt(Instant.now());
            mediaRepository.save(media);
            log.info("✅ Gallery metadata marked deleted: {}", mediaId);

            // Mark ledger entry as deleted (if exists in Gallery)
            List<StorageUsageLedgerDocument> ledgerEntries = storageUsageLedgerRepository.findByDomainRefId(mediaId);
            for (StorageUsageLedgerDocument entry : ledgerEntries) {
                entry.setEndAt(Instant.now());
                storageUsageLedgerRepository.save(entry);
                log.info("📊 Gallery ledger entry marked deleted: {}", entry.getId());
            }

        } catch (Exception e) {
            log.error("❌ Delete failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete media: " + e.getMessage(), e);
        }
    }

    /**
     * List all NON-DELETED media for a user
     */
    public List<MediaItemResponse> listUserMedia(String userId) {
        log.info("📋 Listing media for user: {}", userId);

        List<GalleryMediaDocument> media = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        log.info("Found {} active media items", media.size());

        return media.stream()
                .map(this::mapToResponse)
                .sorted(Comparator.comparing(MediaItemResponse::getUploadedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get storage ledger entries for a date range (for Billing service)
     */
    public List<com.example.Gallery.api.dto.StorageUsageLedgerDTO> getLedgerEntriesBetweenDates(
            Instant startDate, Instant endDate) {
        log.info("📊 Querying ledger entries from {} to {}", startDate, endDate);

        List<StorageUsageLedgerDocument> ledgerEntries =
            storageUsageLedgerRepository.findByStartAtBetween(startDate, endDate);

        log.info("📋 Found {} ledger entries in date range", ledgerEntries.size());

        return ledgerEntries.stream()
                .map(this::mapLedgerToDTO)
                .collect(Collectors.toList());
    }

    // ============ HELPER METHODS ============

    /**
     * Extract metadata from uploaded file
     */
    private Map<String, Object> extractMetadata(MultipartFile file) {
        Map<String, Object> metadata = new HashMap<>();
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
                .build();
    }
}
