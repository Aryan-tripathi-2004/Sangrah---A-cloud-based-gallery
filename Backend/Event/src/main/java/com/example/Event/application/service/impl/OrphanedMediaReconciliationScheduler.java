package com.example.Event.application.service.impl;

import com.example.Event.infrastructure.client.MediaServiceClient;
import com.example.Event.infrastructure.persistence.document.OrphanedMediaLogDocument;
import com.example.Event.infrastructure.persistence.repository.OrphanedMediaLogRepository;
import com.example.Event.shared.enums.OrphanedMediaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled reconciliation worker that recovers and purges orphaned media assets.
 * Solves the dual-write problem by reliably sweeping failed remote compensations asynchronously.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.media.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
public class OrphanedMediaReconciliationScheduler {

    private final OrphanedMediaLogRepository logRepository;
    private final MediaServiceClient mediaServiceClient;

    @Value("${event.media.reconciliation.max-retries:5}")
    private int maxRetries;

    @Scheduled(cron = "${event.media.reconciliation.cron:0 */5 * * * *}")
    public void sweepOrphanedMedia() {
        List<OrphanedMediaLogDocument> pendingLogs = logRepository.findByStatus(OrphanedMediaStatus.PENDING);
        if (pendingLogs.isEmpty()) {
            return;
        }

        log.info("🧹 [OrphanedMediaReconciliation] Found {} pending media sweep tasks", pendingLogs.size());
        for (OrphanedMediaLogDocument item : pendingLogs) {
            String mediaId = item.getOldMediaId();
            String userId = item.getUserId() != null ? item.getUserId() : "SYSTEM";
            try {
                mediaServiceClient.deleteMedia(mediaId, userId);
                item.setStatus(OrphanedMediaStatus.RESOLVED);
                item.setResolvedAt(Instant.now());
                item.setLastAttemptAt(Instant.now());
                logRepository.save(item);
                log.info("✅ [OrphanedMediaReconciliation] Successfully reclaimed orphaned media: {}", mediaId);
            } catch (Exception ex) {
                int attempts = item.getRetryCount() + 1;
                item.setRetryCount(attempts);
                item.setLastAttemptAt(Instant.now());
                item.setErrorMessage(ex.getMessage());
                if (attempts >= maxRetries) {
                    item.setStatus(OrphanedMediaStatus.EXHAUSTED);
                    log.error("❌ [OrphanedMediaReconciliation] Reached max retries ({}) for media {}. Marking EXHAUSTED.", maxRetries, mediaId);
                } else {
                    log.warn("⚠️ [OrphanedMediaReconciliation] Re-attempt {} failed for media {}: {}", attempts, mediaId, ex.getMessage());
                }
                logRepository.save(item);
            }
        }
    }
}
