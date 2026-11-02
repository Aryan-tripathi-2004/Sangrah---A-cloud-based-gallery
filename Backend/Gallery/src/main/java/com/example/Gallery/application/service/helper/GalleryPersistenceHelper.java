package com.example.Gallery.application.service.helper;

import com.example.Gallery.api.dto.response.MediaDeleteResponse;
import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.infrastructure.client.dto.MediaServiceUploadResponse;
import com.example.Gallery.infrastructure.mapper.GalleryMediaMapper;
import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
import com.example.Gallery.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Gallery.infrastructure.persistence.repository.GalleryMediaRepository;
import com.example.Gallery.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import com.example.Gallery.shared.enums.MediaDomain;
import com.example.Gallery.shared.enums.MediaType;
import com.example.Gallery.shared.enums.Visibility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transactional boundary helper for Gallery persistence operations.
 * Resolves the Spring AOP self-invocation proxy bypass anti-pattern.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GalleryPersistenceHelper {

    private final GalleryMediaRepository mediaRepository;
    private final StorageUsageLedgerRepository storageUsageLedgerRepository;
    private final GalleryMediaMapper galleryMediaMapper;

    @Transactional
    public MediaItemResponse persistMediaAndLedger(
            String userId,
            MultipartFile file,
            MediaServiceUploadResponse mediaResponse,
            MediaType type) {
        Instant now = Instant.now();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filename", file.getOriginalFilename());
        metadata.put("contentType", file.getContentType());

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
                .uploadedAt(now)
                .deletedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .metadata(metadata)
                .build();

        GalleryMediaDocument savedGalleryMedia = mediaRepository.save(galleryMedia);
        log.info("Gallery metadata saved: {}", savedGalleryMedia.getId());

        StorageUsageLedgerDocument ledger = StorageUsageLedgerDocument.builder()
                .userId(userId)
                .domain(MediaDomain.GALLERY)
                .domainRefId(savedGalleryMedia.getId())
                .sizeBytes(file.getSize())
                .startAt(now)
                .endAt(null)
                .sourceService("Gallery")
                .createdAt(now)
                .build();

        storageUsageLedgerRepository.save(ledger);
        log.info("Gallery ledger entry saved: {}", ledger.getId());

        return galleryMediaMapper.toMediaItemResponse(savedGalleryMedia);
    }

    @Transactional
    public MediaItemResponse persistMediaMetadata(
            String userId,
            MultipartFile file,
            MediaServiceUploadResponse mediaResponse,
            String mimeType,
            MediaType type,
            Map<String, Object> metadata) {
        Instant now = Instant.now();
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
                .uploadedAt(now)
                .deletedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .metadata(metadata)
                .build();

        GalleryMediaDocument savedGalleryMedia = mediaRepository.save(galleryMedia);
        log.info("Gallery metadata saved: {}", savedGalleryMedia.getId());

        return galleryMediaMapper.toMediaItemResponse(savedGalleryMedia);
    }

    @Transactional
    public MediaDeleteResponse performLocalMediaDeletion(GalleryMediaDocument media, String mediaId) {
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
}
