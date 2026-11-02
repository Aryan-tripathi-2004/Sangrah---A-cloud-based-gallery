package com.example.Gallery.application.service.impl;

import com.example.Gallery.api.dto.StorageUsageLedgerDTO;
import com.example.Gallery.api.dto.response.GalleryMediaListResponse;
import com.example.Gallery.api.dto.response.MediaDeleteResponse;
import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.QuotaResponse;
import com.example.Gallery.api.dto.response.StorageUsageResponse;
import com.example.Gallery.application.service.helper.GalleryPersistenceHelper;
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
import com.example.Gallery.infrastructure.persistence.repository.StorageTypeAggregationResult;
import com.example.Gallery.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import com.example.Gallery.shared.enums.MediaType;
import com.example.Gallery.shared.enums.OrphanedMediaStatus;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GalleryMediaServiceImpl implements IGalleryMediaService {

    private final GalleryMediaRepository mediaRepository;
    private final MediaServiceClient mediaServiceClient;
    private final GalleryMediaMapper galleryMediaMapper;
    private final StorageUsageLedgerRepository storageUsageLedgerRepository;
    private final StorageUsageLedgerMapper ledgerMapper;
    private final OrphanedMediaLogRepository orphanedMediaLogRepository;
    private final GalleryPersistenceHelper galleryPersistenceHelper;

    @Value("${media.storage.max-file-size:104857600}")
    private long maxFileSizeBytes;

    @Override
    public MediaItemResponse uploadMedia(String userId, MultipartFile file) {
        log.info("Starting upload workflow for user: {}, file: {}", userId, file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new DomainValidationException("File cannot be empty");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new DomainValidationException("File size exceeds maximum allowed size of " + (maxFileSizeBytes / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            throw new DomainValidationException("Only image and video files are supported");
        }

        MediaType mediaType = contentType.startsWith("video/") ? MediaType.VIDEO : MediaType.IMAGE;

        log.info("Forwarding file to Media Service: {}", file.getOriginalFilename());
        MediaServiceUploadResponse mediaResponse;
        try {
            mediaResponse = mediaServiceClient.uploadMedia(file, "GALLERY", null, userId);
            log.info("Media Service upload successful, mediaId: {}", mediaResponse.id());
        } catch (Exception e) {
            log.error("Failed to upload to Media Service: {}", e.getMessage(), e);
            throw new MediaProcessingException("Failed to upload media to storage service: " + e.getMessage(), e);
        }

        try {
            return galleryPersistenceHelper.persistMediaAndLedger(userId, file, mediaResponse, mediaType);
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
                            .status(OrphanedMediaStatus.PENDING)
                            .createdAt(Instant.now())
                            .build());
                } catch (Exception orphanLogError) {
                    log.error("Failed to persist orphaned media log for media {}: {}", mediaResponse.id(), orphanLogError.getMessage(), orphanLogError);
                }
            }
            throw ex;
        }
    }

    @Override
    public StorageUsageResponse getStorageUsage(String userId) {
        log.info("📊 Fetching storage usage for user: {}", userId);

        List<StorageTypeAggregationResult> typeBreakdown = mediaRepository.getStorageUsageByType(userId);
        long imageBytes = 0;
        long videoBytes = 0;
        long totalLocalBytes = 0;
        long totalCount = 0;

        for (StorageTypeAggregationResult row : typeBreakdown) {
            totalLocalBytes += row.totalBytes();
            totalCount += row.count();
            if ("IMAGE".equalsIgnoreCase(row._id())) {
                imageBytes += row.totalBytes();
            } else if ("VIDEO".equalsIgnoreCase(row._id())) {
                videoBytes += row.totalBytes();
            }
        }

        long finalTotalBytes = totalLocalBytes;
        try {
            var remote = mediaServiceClient.getStorageUsage(userId);
            if (remote != null) {
                finalTotalBytes = remote.usedBytes();
            }
        } catch (Exception ex) {
            log.warn("⚠️ MediaService getStorageUsage failed, using local aggregation fallback: {}", ex.getMessage());
        }

        return StorageUsageResponse.builder()
                .totalBytesUsed(finalTotalBytes)
                .fileCount(totalCount)
                .breakdown(StorageUsageResponse.StorageBreakdown.builder()
                        .imageBytes(imageBytes)
                        .videoBytes(videoBytes)
                        .build())
                .formatted(StorageUsageResponse.StorageFormatted.builder()
                        .totalFormatted(formatBytes(finalTotalBytes))
                        .imageFormatted(formatBytes(imageBytes))
                        .videoFormatted(formatBytes(videoBytes))
                        .build())
                .message("Storage usage retrieved successfully")
                .build();
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
            throw new MediaProcessingException("Unable to retrieve the requested media file due to an internal service error.", e);
        }
    }

    @Override
    public ResponseEntity<?> getMediaResponse(String userId, String mediaId, String acceptHeader) {
        MediaItemResponse details = getMedia(userId, mediaId);
        if (acceptHeader == null || (!acceptHeader.contains(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
                && !acceptHeader.contains("image/")
                && !acceptHeader.contains("video/"))) {
            return ResponseEntity.ok(details);
        }

        Resource resource = getMediaFile(userId, mediaId);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(details.originalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(org.springframework.http.MediaType.parseMediaType(details.mimeType()))
                .body(resource);
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

        return galleryPersistenceHelper.performLocalMediaDeletion(media, mediaId);
    }

    @Override
    public GalleryMediaListResponse listUserMedia(String userId) {
        log.info("📋 Listing media for user: {}", userId);

        List<GalleryMediaDocument> mediaList = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        log.info("Found {} active files", mediaList.size());

        List<MediaItemResponse> responses = mediaList.stream()
                .map(galleryMediaMapper::toMediaItemResponse)
                .collect(Collectors.toList());

        return new GalleryMediaListResponse(responses.size(), responses);
    }

    @Override
    public QuotaResponse getQuota() {
        log.info("?? Fetching quota descriptor");
        return new QuotaResponse("Deprecated: Use /api/v1/gallery/storage-usage instead", 10737418240L, 0L, true, "/api/v1/gallery/storage-usage");
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

    @Override
    public Page<StorageUsageLedgerDTO> getLedgerEntriesBetweenDates(Instant startDate, Instant endDate, int page, int size) {
        log.info("Querying paginated ledger entries from {} to {} (page: {}, size: {})", startDate, endDate, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<StorageUsageLedgerDocument> ledgerPage =
            storageUsageLedgerRepository.findByStartAtBetween(startDate, endDate, pageable);

        log.info("Found {} ledger entries on page {}/{}", ledgerPage.getNumberOfElements(), page, ledgerPage.getTotalPages());

        return ledgerPage.map(ledgerMapper::toDTO);
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
