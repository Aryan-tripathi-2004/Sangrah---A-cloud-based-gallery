package com.example.Billing.infrastructure.feign;

import com.example.Billing.api.dto.response.StorageUsageLedgerDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;

/**
 * Feign Client for Gallery Service
 * Provides inter-service communication with the Gallery microservice
 * to query storage usage ledger data without direct database access
 *
 * This follows microservice principles:
 * - Gallery owns its own data store (sangrah_gallery)
 * - Billing queries Gallery via REST API (not direct DB access)
 * - Loose coupling between services
 * - Easy to scale/deploy independently
 */
@FeignClient(
    name = "gallery-service",
    url = "${gallery.service.url:http://localhost:8082}"
)
public interface GalleryServiceClient {

    /**
     * Query storage ledger entries for a date range with pagination
     * @param startDate Start of date range (inclusive)
     * @param endDate End of date range (inclusive)
     * @param page Page index (0-based)
     * @param size Page size
     * @return Paginated storage usage ledger entries
     */
    @GetMapping("/api/v1/gallery/ledger")
    Page<StorageUsageLedgerDTO> getLedgerEntries(
        @RequestParam("startDate") Instant startDate,
        @RequestParam("endDate") Instant endDate,
        @RequestParam("page") int page,
        @RequestParam("size") int size
    );

    /**
     * Health check - verify Gallery service is up
     */
    @GetMapping("/api/v1/gallery/health")
    String health();
}
