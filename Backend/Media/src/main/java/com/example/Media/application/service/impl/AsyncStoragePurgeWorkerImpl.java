package com.example.Media.application.service.impl;

import com.example.Media.application.service.interfaces.IAsyncStoragePurgeWorker;
import com.example.Media.infrastructure.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Asynchronous worker implementation for purging physical files from storage.
 * Executes on a dedicated background thread pool to prevent HTTP thread starvation
 * and avoid holding open database transactions during remote or disk I/O.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncStoragePurgeWorkerImpl implements IAsyncStoragePurgeWorker {

    private final StorageProvider storageProvider;

    @Override
    @Async("mediaTaskExecutor")
    public void purgeStorageKeysAsync(List<String> storageKeys) {
        if (storageKeys == null || storageKeys.isEmpty()) {
            return;
        }

        log.info("🚀 [Async Purge Worker] Starting background purge of {} storage keys on thread: {}",
                storageKeys.size(), Thread.currentThread().getName());

        int successCount = 0;
        int failureCount = 0;

        for (String storageKey : storageKeys) {
            if (storageKey == null || storageKey.isBlank()) {
                continue;
            }
            try {
                storageProvider.delete(storageKey);
                successCount++;
                log.debug("🗑️ [Async Purge Worker] Physical file purged: {}", storageKey);
            } catch (IOException e) {
                failureCount++;
                log.error("❌ [Async Purge Worker] Failed to purge storageKey {}: {}", storageKey, e.getMessage(), e);
            } catch (Exception e) {
                failureCount++;
                log.error("❌ [Async Purge Worker] Unexpected error purging storageKey {}: {}", storageKey, e.getMessage(), e);
            }
        }

        log.info("🏁 [Async Purge Worker] Completed background purge. Success: {}, Failures: {}, Total: {}",
                successCount, failureCount, storageKeys.size());
    }

    @Override
    @Async("mediaTaskExecutor")
    public void purgeStorageKeyAsync(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }

        log.info("🚀 [Async Purge Worker] Starting background purge for key {} on thread: {}",
                storageKey, Thread.currentThread().getName());

        try {
            storageProvider.delete(storageKey);
            log.info("🗑️ [Async Purge Worker] Physical file purged: {}", storageKey);
        } catch (IOException e) {
            log.error("❌ [Async Purge Worker] Failed to purge storageKey {}: {}", storageKey, e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ [Async Purge Worker] Unexpected error purging storageKey {}: {}", storageKey, e.getMessage(), e);
        }
    }
}
