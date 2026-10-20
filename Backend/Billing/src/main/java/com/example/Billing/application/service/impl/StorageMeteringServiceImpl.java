package com.example.Billing.application.service.impl;

import com.example.Billing.api.dto.response.StorageUsageLedgerDTO;
import com.example.Billing.api.dto.response.StorageUsageSummaryResponse;
import com.example.Billing.application.service.interfaces.IStorageMeteringService;
import com.example.Billing.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Billing.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Value("${billing.storage.limit.bytes}")
    private long storageLimitBytes;

    @Override
    public StorageUsageSummaryResponse getStorageUsageSummary(String userId) {
        log.info("📊 [Storage Metering] Fetching storage summary for user: {}", userId);
        List<StorageUsageLedgerDocument> usageRecords = storageUsageLedgerRepository.findByUserId(userId);
        long totalBytesUsed = usageRecords.stream()
                .mapToLong(StorageUsageLedgerDocument::getSizeBytes)
                .sum();
        return new StorageUsageSummaryResponse(totalBytesUsed, storageLimitBytes);
    }

    @Override
    public List<StorageUsageLedgerDTO> getStorageUsageDetails(String userId) {
        log.info("📊 [Storage Metering] Fetching detailed storage usage records for user: {}", userId);
        List<StorageUsageLedgerDocument> usageRecords = storageUsageLedgerRepository.findByUserId(userId);
        return usageRecords.stream()
                .map(doc -> new StorageUsageLedgerDTO(
                        doc.getId(),
                        doc.getUserId(),
                        doc.getDomain(),
                        doc.getDomainRefId(),
                        doc.getSizeBytes(),
                        doc.getStartAt(),
                        doc.getEndAt(),
                        doc.getSourceService(),
                        doc.getCreatedAt()
                ))
                .toList();
    }
}
