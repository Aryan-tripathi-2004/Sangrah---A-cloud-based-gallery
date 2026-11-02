package com.example.Media.application.service.impl;

import com.example.Media.application.service.interfaces.IMediaStorageGarbageCollectionService;
import com.example.Media.infrastructure.persistence.document.MediaDocument;
import com.example.Media.infrastructure.persistence.repository.MediaRepository;
import com.example.Media.infrastructure.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Scheduled garbage collector that sweeps soft-deleted media assets past the retention threshold.
 *
 * <p>Serves as a resilient safety net for the in-memory asynchronous purge worker.
 * If the JVM restarts, crashes, or hits queue capacity before the {@code mediaTaskExecutor}
 * can execute physical file deletion, this job discovers unpurged assets and removes them
 * from physical storage.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "media.gc.enabled", havingValue = "true", matchIfMissing = true)
public class MediaStorageGarbageCollectionServiceImpl implements IMediaStorageGarbageCollectionService {

    private final MediaRepository mediaRepository;
    private final StorageProvider storageProvider;

    @Value("${media.gc.retention-days}")
    private long retentionDays;

    @Override
    @Scheduled(cron = "${media.gc.cron:0 0 2 * * *}")
    public void purgeOrphanedStorageFiles() {
        sweepOrphanedStorageFiles();
    }

    @Override
    public int sweepOrphanedStorageFiles() {
        Instant cutoffDate = Instant.now().minus(Duration.ofDays(retentionDays));
        log.info("🧹 [Media GC] Starting storage sweep for media soft-deleted before {} (retention: {} days)",
                cutoffDate, retentionDays);

        List<MediaDocument> candidateDocs;
        try {
            candidateDocs = mediaRepository.findByDeletedAtBeforeAndStorageKeyIsNotNull(cutoffDate);
        } catch (Exception e) {
            log.error("❌ [Media GC] Failed to query soft-deleted media documents: {}", e.getMessage(), e);
            return 0;
        }

        if (candidateDocs.isEmpty()) {
            log.info("✅ [Media GC] No orphaned media documents pending physical purge.");
            return 0;
        }

        log.info("📋 [Media GC] Discovered {} candidate soft-deleted media documents for physical purge",
                candidateDocs.size());

        int purgedCount = 0;
        int alreadyPurgedCount = 0;
        int failedCount = 0;

        for (MediaDocument doc : candidateDocs) {
            String storageKey = doc.getStorageKey();
            if (storageKey == null || storageKey.isBlank()) {
                continue;
            }

            try {
                if (storageProvider.exists(storageKey)) {
                    storageProvider.delete(storageKey);
                    purgedCount++;
                    log.debug("🗑️ [Media GC] Physically purged file from storage: {}", storageKey);
                } else {
                    alreadyPurgedCount++;
                    log.debug("ℹ️ [Media GC] File already absent from storage (purged by async worker): {}", storageKey);
                }

                // Clear storageKey to prevent redundant checks on subsequent sweeps
                doc.setStorageKey(null);
                doc.setUpdatedAt(Instant.now());
                mediaRepository.save(doc);

            } catch (IOException e) {
                failedCount++;
                log.error("❌ [Media GC] Failed to delete storage file for key {}: {}", storageKey, e.getMessage(), e);
            } catch (Exception e) {
                failedCount++;
                log.error("❌ [Media GC] Unexpected error processing key {}: {}", storageKey, e.getMessage(), e);
            }
        }

        log.info("🏁 [Media GC] Sweep completed. Purged: {}, Already absent: {}, Failed: {}, Total evaluated: {}",
                purgedCount, alreadyPurgedCount, failedCount, candidateDocs.size());
        return purgedCount;
    }
}
