package com.example.Gallery.api.controller;

import com.example.Gallery.api.dto.StorageUsageLedgerDTO;
import com.example.Gallery.application.service.interfaces.IGalleryMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/gallery")
@RequiredArgsConstructor
@Tag(name = "Gallery Ledger", description = "Storage ledger endpoints for inter-service communication")
public class GalleryLedgerController {

    private final IGalleryMediaService mediaService;

    @GetMapping("/ledger")
    @Operation(summary = "Get storage ledger entries", description = "Retrieve storage usage ledger entries within a date range. Used by Billing service for invoice generation.")
    public ResponseEntity<List<StorageUsageLedgerDTO>> getLedgerEntries(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {

        log.info("📋 Ledger query from {} to {}", startDate, endDate);
        List<StorageUsageLedgerDTO> entries = mediaService.getLedgerEntriesBetweenDates(startDate, endDate);
        log.info("✅ Returned {} ledger entries", entries.size());
        return ResponseEntity.ok(entries);
    }
}
