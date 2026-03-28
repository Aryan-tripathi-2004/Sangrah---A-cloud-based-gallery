package com.example.Gallery.api.controller;

import com.example.Gallery.api.dto.StorageUsageLedgerDTO;
import com.example.Gallery.application.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Gallery Ledger API
 * Exposes storage usage ledger data for other services (e.g., Billing)
 * This follows microservice principle: Gallery owns and exposes its own data
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gallery")
@RequiredArgsConstructor
@Tag(name = "Gallery Ledger", description = "Storage ledger endpoints for inter-service communication")
public class GalleryLedgerController {

    private final MediaService mediaService;

    /**
     * Get storage usage ledger entries for a date range
     * Used by Billing service to calculate invoices
     *
     * Query: GET /api/v1/gallery/ledger?startDate=2026-03-01T00:00:00Z&endDate=2026-03-31T23:59:59Z
     */
    @GetMapping("/ledger")
    @Operation(summary = "Get storage ledger entries", description = "Retrieve storage usage ledger entries within a date range. Used by Billing service for invoice generation.")
    public ResponseEntity<List<StorageUsageLedgerDTO>> getLedgerEntries(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {

        log.info("📋 Ledger query from {} to {}", startDate, endDate);

        try {
            List<StorageUsageLedgerDTO> entries = mediaService.getLedgerEntriesBetweenDates(startDate, endDate);
            log.info("✅ Returned {} ledger entries", entries.size());
            return ResponseEntity.ok(entries);

        } catch (Exception e) {
            log.error("❌ Failed to fetch ledger entries", e);
            return ResponseEntity.status(500).build();
        }
    }
}
