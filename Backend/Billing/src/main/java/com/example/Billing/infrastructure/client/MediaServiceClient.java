package com.example.Billing.infrastructure.client;

import com.example.Billing.api.dto.response.StorageUsageLedgerDTO;
import com.example.Billing.infrastructure.client.dto.MediaStorageUsageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;

/**
 * Feign client for the Media microservice.
 * MediaService is the single source of truth for physical media storage
 * and the primary storage usage ledger.
 */
@FeignClient(
    name = "media-service",
    url = "${media.service.url:http://localhost:8087}"
)
public interface MediaServiceClient {

    @GetMapping("/internal/api/v1/media/usage")
    MediaStorageUsageDTO getStorageUsage(@RequestParam("userId") String userId);

    @GetMapping("/internal/api/v1/media/ledger/user")
    List<StorageUsageLedgerDTO> getUserStorageLedger(
        @RequestParam("userId") String userId,
        @RequestParam("startDate") Instant startDate,
        @RequestParam("endDate") Instant endDate
    );

    @GetMapping("/internal/api/v1/media/ledger")
    Page<StorageUsageLedgerDTO> getLedgerEntries(
        @RequestParam("startDate") Instant startDate,
        @RequestParam("endDate") Instant endDate,
        @RequestParam("page") int page,
        @RequestParam("size") int size
    );
}
