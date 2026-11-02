package com.example.Billing.application.service.interfaces;

import com.example.Billing.api.dto.response.StorageUsageLedgerDTO;
import com.example.Billing.api.dto.response.StorageUsageSummaryResponse;

import java.util.List;

/**
 * Service contract for storage metering and ledger data aggregation.
 * Decouples storage usage analytics and ledger queries from invoice lifecycle management.
 */
public interface IStorageMeteringService {

    /**
     * Retrieves the summarised storage usage (total bytes used and storage limit) for a user.
     *
     * @param userId user identifier
     * @return storage usage summary response
     */
    StorageUsageSummaryResponse getStorageUsageSummary(String userId);

    /**
     * Retrieves the detailed per-file storage usage ledger records for a user.
     *
     * @param userId user identifier
     * @return list of storage usage ledger DTOs
     */
    List<StorageUsageLedgerDTO> getStorageUsageDetails(String userId);
}
