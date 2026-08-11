package com.example.Gallery.application.service.impl;

import com.example.Gallery.api.dto.StorageUsageLedgerDTO;
import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.StorageUsageResponse;
import com.example.Gallery.application.service.interfaces.IGalleryMediaService;
import com.example.Gallery.infrastructure.client.MediaServiceClient;
import com.example.Gallery.infrastructure.client.dto.MediaServiceUploadResponse;
import com.example.Gallery.infrastructure.mapper.GalleryMediaMapper;
import com.example.Gallery.infrastructure.mapper.StorageUsageLedgerMapper;
import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
import com.example.Gallery.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Gallery.infrastructure.persistence.repository.GalleryMediaRepository;
import com.example.Gallery.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import com.example.Gallery.shared.enums.MediaType;
import com.example.Gallery.shared.enums.Visibility;
import com.example.Gallery.shared.exception.DomainValidationException;
import com.example.Gallery.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GalleryMediaServiceImpl implements IGalleryMediaService {

    private final GalleryMediaRepository mediaRepository;
    private final StorageUsageLedgerRepository storageUsageLedgerRepository;
    private final MediaServiceClient mediaServiceClient;
    private final GalleryMediaMapper galleryMediaMapper;
    private final StorageUsageLedgerMapper ledgerMapper;

    @Override
    @Transactional
    public MediaItemResponse uploadMedia(String userId, MultipartFile file) throws IOException {
        log.info("📤 Uploading media for user: {} via Media Service, file: {}", userId, file.getOriginalFilename());

        try {
            MediaServiceUploadResponse mediaResponse = mediaServiceClient.uploadMedia(
                    file,
                    "GALLERY",
                    null,
                    userId
            );
            log.info("✅ File uploaded to Media Service, response: {}", mediaResponse);

            MediaType type = file.getContentType() != null && file.getContentType().startsWith("video/") 
                    ? MediaType.VIDEO 
                    : MediaType.IMAGE;
            
            GalleryMediaDocument galleryMedia = GalleryMediaDocument.builder()
                    .id(mediaResponse.id())
                    .userId(userId)
                    .ownerUserId(userId)
                    .originalFileName(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .storageKey(mediaResponse.storageKey())
                    .storageProvider("MEDIA_SERVICE")
                    .checksumSha256(mediaResponse.checksumSha256())
                    .type(type)
                    .visibility(Visibility.PRIVATE)
                    .uploadedAt(Instant.now())
                    .deletedAt(null)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .metadata(extractMetadata(file))
                    .build();

            GalleryMediaDocument savedGalleryMedia = mediaRepository.save(galleryMedia);
            log.info("✅ Gallery metadata saved: {}", savedGalleryMedia.getId());

            return galleryMediaMapper.toMediaItemResponse(savedGalleryMedia);

        } catch (Exception e) {
            log.error("❌ Upload to Media Service failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Override
    public StorageUsageResponse getStorageUsage(String userId) {
        log.info("📊 Fetching storage usage for user: {}", userId);

        List<GalleryMediaDocument> activeMedia = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        log.info("Found {} active files for user {}", activeMedia.size(), userId);

        List<MediaItemResponse> mediaResponses = activeMedia.stream()
                .map(galleryMediaMapper::toMediaItemResponse)
                .collect(Collectors.toList());

        StorageUsageResponse response = buildStorageUsageResponse(mediaResponses);

        log.info("📊 Storage stats: {} bytes, {} files", response.totalBytesUsed(), response.fileCount());
        return response;
    }

    @Override
    public MediaItemResponse getMedia(String userId, String mediaId) {
        log.info("🔍 Fetching media {} for user {}", mediaId, userId);

        GalleryMediaDocument media = mediaRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found or access denied"));

        return galleryMediaMapper.toMediaItemResponse(media);
    }

    @Override
    public Resource getMediaFile(String userId, String mediaId) {
        log.info("📥 Fetching media file {} for user {}", mediaId, userId);

        GalleryMediaDocument media = mediaRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found or access denied"));

        if (media.isDeleted()) {
            throw new DomainValidationException("Media has been deleted");
        }

        try {
            byte[] fileBytes = mediaServiceClient.getMediaFile(mediaId, userId);
            log.info("✅ File retrieved from Media Service: {} bytes", fileBytes.length);

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

    @Override
    @Transactional
    public void deleteMedia(String userId, String mediaId) {
        log.info("🗑️ Deleting media {} for user {}", mediaId, userId);

        GalleryMediaDocument media = mediaRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found or access denied"));

        if (media.isDeleted()) {
            log.warn("⚠️ Media already deleted: {}", mediaId);
            return;
        }

        try {
            mediaServiceClient.deleteMedia(mediaId, userId);
            log.info("✅ Media Service deletion confirmed");

            media.setDeletedAt(Instant.now());
            media.setUpdatedAt(Instant.now());
            mediaRepository.save(media);
            log.info("✅ Gallery metadata marked deleted: {}", mediaId);

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

    @Override
    public List<MediaItemResponse> listUserMedia(String userId) {
        log.info("📋 Listing media for user: {}", userId);

        List<GalleryMediaDocument> media = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        log.info("Found {} active media items", media.size());

        return media.stream()
                .map(galleryMediaMapper::toMediaItemResponse)
                .sorted(Comparator.comparing(MediaItemResponse::uploadedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<StorageUsageLedgerDTO> getLedgerEntriesBetweenDates(Instant startDate, Instant endDate) {
        log.info("📊 Querying ledger entries from {} to {}", startDate, endDate);

        List<StorageUsageLedgerDocument> ledgerEntries =
            storageUsageLedgerRepository.findByStartAtBetween(startDate, endDate);

        log.info("📋 Found {} ledger entries in date range", ledgerEntries.size());

        return ledgerEntries.stream()
                .map(ledgerMapper::toDTO)
                .collect(Collectors.toList());
    }

    private Map<String, Object> extractMetadata(MultipartFile file) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filename", file.getOriginalFilename());
        metadata.put("contentType", file.getContentType());
        return metadata;
    }

    private StorageUsageResponse buildStorageUsageResponse(List<MediaItemResponse> files) {
        long totalBytes = 0;
        long imageBytes = 0;
        long videoBytes = 0;

        for (MediaItemResponse file : files) {
            totalBytes += file.sizeBytes();
            if (file.type() == MediaType.IMAGE) {
                imageBytes += file.sizeBytes();
            } else if (file.type() == MediaType.VIDEO) {
                videoBytes += file.sizeBytes();
            }
        }

        return new StorageUsageResponse(
                totalBytes,
                files.size(),
                imageBytes,
                videoBytes,
                null, // oldestFile logic could go here
                null, // newestFile logic could go here
                formatBytes(totalBytes)
        );
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
