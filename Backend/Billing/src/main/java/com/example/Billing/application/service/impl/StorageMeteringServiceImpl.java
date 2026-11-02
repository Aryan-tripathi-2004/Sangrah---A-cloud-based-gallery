package com.example.Billing.application.service.impl;

import com.example.Billing.api.dto.response.StorageUsageLedgerDTO;
import com.example.Billing.api.dto.response.StorageUsageSummaryResponse;
import com.example.Billing.application.service.interfaces.IStorageMeteringService;
import com.example.Billing.infrastructure.client.MediaServiceClient;
import com.example.Billing.infrastructure.client.dto.MediaStorageUsageDTO;
import com.example.Billing.infrastructure.mapper.StorageUsageMapper;
import com.example.Billing.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Billing.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service implementation for storage metering and ledger data aggregation.
 * Encapsulates calculation of user storage consumption and retrieval of audit ledger details.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageMeteringServiceImpl implements IStorageMeteringService {

    private final StorageUsageLedgerRepository storageUsageLedgerRepository;
    private final MediaServiceClient mediaServiceClient;
    private final StorageUsageMapper storageUsageMapper;

    @Value("${billing.storage.limit.bytes}")
    private long storageLimitBytes;

    @Override
    public StorageUsageSummaryResponse getStorageUsageSummary(String userId) {
        log.info("📊 [Storage Metering] Fetching storage summary for user: {}", userId);
        try {
            MediaStorageUsageDTO remoteUsage = mediaServiceClient.getStorageUsage(userId);
            if (remoteUsage != null && remoteUsage.usedBytes() != null) {
                return new StorageUsageSummaryResponse(remoteUsage.usedBytes(),
                        remoteUsage.limitBytes() != null ? remoteUsage.limitBytes() : storageLimitBytes);
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to fetch remote storage summary from MediaService, falling back to local: {}", e.getMessage());
        }

        List<StorageUsageLedgerDocument> usageRecords = storageUsageLedgerRepository.findByUserId(userId);
        long totalBytesUsed = usageRecords.stream()
                .mapToLong(StorageUsageLedgerDocument::getSizeBytes)
                .sum();
        return new StorageUsageSummaryResponse(totalBytesUsed, storageLimitBytes);
    }

    @Override
    public List<StorageUsageLedgerDTO> getStorageUsageDetails(String userId) {
        log.info("📊 [Storage Metering] Fetching detailed storage usage records for user: {}", userId);
        try {
            List<StorageUsageLedgerDTO> remoteEntries = mediaServiceClient.getUserStorageLedger(userId, Instant.EPOCH, Instant.now());
            if (remoteEntries != null && !remoteEntries.isEmpty()) {
                return remoteEntries;
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to fetch remote storage ledger from MediaService, falling back to local: {}", e.getMessage());
        }

        List<StorageUsageLedgerDocument> usageRecords = storageUsageLedgerRepository.findByUserId(userId);
        return usageRecords.stream()
                .map(storageUsageMapper::toLedgerDTO)
                .toList();
    }
}
