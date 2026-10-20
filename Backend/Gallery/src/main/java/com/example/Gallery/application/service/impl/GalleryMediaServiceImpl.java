package com.example.Gallery.application.service.impl;

import com.example.Gallery.api.dto.StorageUsageLedgerDTO;
import com.example.Gallery.api.dto.response.GalleryMediaListResponse;
import com.example.Gallery.api.dto.response.MediaDeleteResponse;
import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.QuotaResponse;
import com.example.Gallery.api.dto.response.StorageUsageResponse;
import com.example.Gallery.application.service.interfaces.IGalleryMediaService;
import com.example.Gallery.infrastructure.client.MediaServiceClient;
import com.example.Gallery.infrastructure.client.dto.MediaServiceUploadResponse;
import com.example.Gallery.infrastructure.mapper.GalleryMediaMapper;
import com.example.Gallery.infrastructure.mapper.StorageUsageLedgerMapper;
import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
import com.example.Gallery.infrastructure.persistence.document.OrphanedMediaLogDocument;
import com.example.Gallery.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Gallery.infrastructure.persistence.repository.GalleryMediaRepository;
import com.example.Gallery.infrastructure.persistence.repository.OrphanedMediaLogRepository;
import com.example.Gallery.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import com.example.Gallery.shared.enums.MediaType;
import com.example.Gallery.shared.enums.Visibility;
import com.example.Gallery.shared.exception.DomainValidationException;
import com.example.Gallery.shared.exception.MediaProcessingException;
import com.example.Gallery.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.tika.Tika;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GalleryMediaServiceImpl implements IGalleryMediaService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "video/mp4", "video/webm"
    );

    private final Tika tika;

    private final GalleryMediaRepository mediaRepository;
    private final StorageUsageLedgerRepository storageUsageLedgerRepository;
    private final OrphanedMediaLogRepository orphanedMediaLogRepository;
    private final MediaServiceClient mediaServiceClient;
    private final GalleryMediaMapper galleryMediaMapper;
    private final StorageUsageLedgerMapper ledgerMapper;

    @Value("${gallery.upload.max-size}")
    private long maxFileSize;

    @Override
    public MediaItemResponse uploadMedia(String userId, MultipartFile file) throws IOException {
        log.info("Uploading media for user: {} via Media Service, file: {}", userId, file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new DomainValidationException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new DomainValidationException("File too large. Max allowed: " + maxFileSize + " bytes");
        }

        String mimeType;
        try {
            mimeType = tika.detect(file.getInputStream());
        } catch (Exception e) {
            log.error("Failed to detect MIME type for file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new DomainValidationException("Unable to read or parse file MIME type", e);
        }

        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new DomainValidationException("Unsupported or malicious file type detected: " + mimeType);
        }

        MediaServiceUploadResponse mediaResponse;
        try {
            mediaResponse = mediaServiceClient.uploadMedia(
                    file,
                    "GALLERY",
                    null,
                    userId
            );
            log.info("File uploaded to Media Service, response: {}", mediaResponse);
        } catch (DomainValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Upload to Media Service failed: {}", e.getMessage(), e);
            throw new MediaProcessingException("Failed to upload file due to an internal service communication error.", e);
        }

        if (mediaResponse == null || mediaResponse.id() == null || mediaResponse.id().isBlank()) {
            throw new MediaProcessingException("Media Service upload returned an empty or invalid media ID");
        }

        MediaType type = mimeType.startsWith("video/") ? MediaType.VIDEO : MediaType.IMAGE;
        try {
            return persistMediaMetadata(userId, file, mediaResponse, mimeType, type);
        } catch (Exception ex) {
            log.error("Failed to persist gallery media metadata for media {}. Compensating by deleting remote media: {}", mediaResponse.id(), ex.getMessage());
            try {
                mediaServiceClient.deleteMedia(mediaResponse.id(), userId);
                log.warn("🧹 Cleaned up orphaned remote media {} after local database failure", mediaResponse.id());
            } catch (Exception cleanupError) {
                log.error("Compensating deleteMedia failed for media {}: {}", mediaResponse.id(), cleanupError.getMessage(), cleanupError);
                try {
                    orphanedMediaLogRepository.save(OrphanedMediaLogDocument.builder()
                            .userId(userId)
                            .mediaId(mediaResponse.id())
                            .errorMessage(cleanupError.getMessage())
                            .createdAt(Instant.now())
                            .build());
                } catch (Exception orphanLogError) {
                    log.error("Failed to persist orphaned media log for media {}: {}", mediaResponse.id(), orphanLogError.getMessage(), orphanLogError);
                }
            }
            throw ex;
        }
    }

    @Transactional
    protected MediaItemResponse persistMediaMetadata(
            String userId,
            MultipartFile file,
            MediaServiceUploadResponse mediaResponse,
            String mimeType,
            MediaType type) {
        GalleryMediaDocument galleryMedia = GalleryMediaDocument.builder()
                .id(mediaResponse.id())
                .userId(userId)
                .ownerUserId(userId)
                .originalFileName(file.getOriginalFilename())
                .mimeType(mimeType)
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
        log.info("Gallery metadata saved: {}", savedGalleryMedia.getId());

        return galleryMediaMapper.toMediaItemResponse(savedGalleryMedia);
    }

    @Override
    public StorageUsageResponse getStorageUsage(String userId) {
        log.info("ðŸ“Š Fetching storage usage for user: {}", userId);

        List<GalleryMediaDocument> activeMedia = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        log.info("Found {} active files for user {}", activeMedia.size(), userId);

        List<MediaItemResponse> mediaResponses = activeMedia.stream()
                .map(galleryMediaMapper::toMediaItemResponse)
                .collect(Collectors.toList());

        StorageUsageResponse response = buildStorageUsageResponse(mediaResponses);

        log.info("ðŸ“Š Storage stats: {} bytes, {} files", response.totalBytesUsed(), response.fileCount());
        return response;
    }

    @Override
    public MediaItemResponse getMedia(String userId, String mediaId) {
        log.info("ðŸ” Fetching media {} for user {}", mediaId, userId);

        GalleryMediaDocument media = mediaRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found or access denied"));

        return galleryMediaMapper.toMediaItemResponse(media);
    }

    @Override
    public Resource getMediaFile(String userId, String mediaId) {
        log.info("ðŸ“¥ Fetching media file {} for user {}", mediaId, userId);

        GalleryMediaDocument media = mediaRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found or access denied"));

        if (media.isDeleted()) {
            throw new DomainValidationException("Media has been deleted");
        }

        try {
            byte[] fileBytes = mediaServiceClient.getMediaFile(mediaId, userId);
            log.info("âœ… File retrieved from Media Service: {} bytes", fileBytes.length);

            return new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return media.getOriginalFileName();
                }
            };
        } catch (Exception e) {
            log.error("âŒ Failed to fetch file from Media Service: {}", e.getMessage(), e);
            throw new MediaProcessingException("Unable to retrieve the requested media file due to an internal service error.", e);
        }
    }

    @Override
    public MediaDeleteResponse deleteMedia(String userId, String mediaId) {
        log.info("Deleting media {} for user {}", mediaId, userId);

        GalleryMediaDocument media = mediaRepository.findByIdAndUserId(mediaId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found or access denied"));

        if (media.isDeleted()) {
            log.warn("Media already deleted: {}", mediaId);
            return new MediaDeleteResponse("Media already deleted", mediaId);
        }

        try {
            mediaServiceClient.deleteMedia(mediaId, userId);
            log.info("Media Service deletion confirmed");
        } catch (Exception e) {
            log.error("Delete failed on Media Service: {}", e.getMessage(), e);
            throw new MediaProcessingException("Unable to delete media file due to an internal service error.", e);
        }

        return performLocalMediaDeletion(media, mediaId);
    }

    @Transactional
    protected MediaDeleteResponse performLocalMediaDeletion(GalleryMediaDocument media, String mediaId) {
        Instant now = Instant.now();
        media.setDeletedAt(now);
        media.setUpdatedAt(now);
        mediaRepository.save(media);
        log.info("Gallery metadata marked deleted: {}", mediaId);

        List<StorageUsageLedgerDocument> ledgerEntries = storageUsageLedgerRepository.findByDomainRefId(mediaId);
        List<StorageUsageLedgerDocument> entriesToUpdate = new ArrayList<>();
        for (StorageUsageLedgerDocument entry : ledgerEntries) {
            if (entry.isActive()) {
                entry.setEndAt(now);
                entriesToUpdate.add(entry);
                log.info("Gallery ledger entry marked deleted: {}", entry.getId());
            }
        }

        if (!entriesToUpdate.isEmpty()) {
            storageUsageLedgerRepository.saveAll(entriesToUpdate);
        }

        return new MediaDeleteResponse("Media deleted successfully", mediaId);
    }

    @Override
    public GalleryMediaListResponse listUserMedia(String userId) {
        log.info("ðŸ“‹ Listing media for user: {}", userId);

        List<GalleryMediaDocument> media = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        log.info("Found {} active media items", media.size());

        List<MediaItemResponse> items = media.stream()
                .map(galleryMediaMapper::toMediaItemResponse)
                .sorted(Comparator.comparing(MediaItemResponse::uploadedAt).reversed())
                .collect(Collectors.toList());

        return new GalleryMediaListResponse(items.size(), items);
    }

    @Override
    public QuotaResponse getQuota() {
        return new QuotaResponse(
                "PAY-AS-YOU-USE model: No storage quotas",
                -1L,
                0L,
                true,
                "/api/v1/gallery/usage"
        );
    }

    @Override
    public List<StorageUsageLedgerDTO> getLedgerEntriesBetweenDates(Instant startDate, Instant endDate) {
        log.info("ðŸ“Š Querying ledger entries from {} to {}", startDate, endDate);

        List<StorageUsageLedgerDocument> ledgerEntries =
            storageUsageLedgerRepository.findByStartAtBetween(startDate, endDate);

        log.info("ðŸ“‹ Found {} ledger entries in date range", ledgerEntries.size());

        return ledgerEntries.stream()
                .map(ledgerMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<StorageUsageLedgerDTO> getLedgerEntriesBetweenDates(Instant startDate, Instant endDate, int page, int size) {
        log.info("Querying paginated ledger entries from {} to {} (page: {}, size: {})", startDate, endDate, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<StorageUsageLedgerDocument> ledgerPage =
            storageUsageLedgerRepository.findByStartAtBetween(startDate, endDate, pageable);

        log.info("Found {} ledger entries on page {}/{}", ledgerPage.getNumberOfElements(), page, ledgerPage.getTotalPages());

        return ledgerPage.map(ledgerMapper::toDTO);
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

        return StorageUsageResponse.builder()
                .totalBytesUsed(totalBytes)
                .fileCount(files.size())
                .breakdown(StorageUsageResponse.StorageBreakdown.builder()
                        .imageBytes(imageBytes)
                        .videoBytes(videoBytes)
                        .build())
                .formatted(StorageUsageResponse.StorageFormatted.builder()
                        .totalFormatted(formatBytes(totalBytes))
                        .imageFormatted(formatBytes(imageBytes))
                        .videoFormatted(formatBytes(videoBytes))
                        .build())
                .message("Storage usage retrieved successfully")
                .build();
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
