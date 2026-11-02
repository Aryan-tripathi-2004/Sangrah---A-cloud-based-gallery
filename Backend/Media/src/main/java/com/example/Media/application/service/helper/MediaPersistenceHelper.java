package com.example.Media.application.service.helper;

import com.example.Media.api.dto.response.MediaResponse;
import com.example.Media.infrastructure.mapper.MediaMapper;
import com.example.Media.infrastructure.persistence.document.MediaDocument;
import com.example.Media.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Media.infrastructure.persistence.repository.MediaRepository;
import com.example.Media.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import com.example.Media.shared.enums.MediaDomain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

/**
 * Transactional boundary helper for Media persistence operations.
 * Resolves the Spring AOP self-invocation proxy bypass anti-pattern.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaPersistenceHelper {

    private final MediaRepository mediaRepository;
    private final StorageUsageLedgerRepository ledgerRepository;
    private final MediaMapper mediaMapper;

    /**
     * Persist metadata and ledger entry within a genuine Spring AOP transactional boundary.
     */
    @Transactional
    public MediaResponse persistMediaRecords(
            String userId,
            MediaDomain domain,
            String entityRefId,
            MultipartFile file,
            String checksum,
            String mimeType,
            String storageKey,
            String storageProviderName) {
        Instant now = Instant.now();

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
}
