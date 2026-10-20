package com.example.Gallery.api.controller;

import com.example.Gallery.api.dto.StorageUsageLedgerDTO;
import com.example.Gallery.application.service.interfaces.IGalleryMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/v1/gallery")
@RequiredArgsConstructor
@Tag(name = "Gallery Ledger", description = "Storage ledger endpoints for inter-service communication")
public class GalleryLedgerController {

    private final IGalleryMediaService mediaService;

    @GetMapping("/ledger")
    @Operation(summary = "Get storage ledger entries", description = "Retrieve storage usage ledger entries within a date range with pagination support. Used by Billing service for invoice generation.")
    public ResponseEntity<Page<StorageUsageLedgerDTO>> getLedgerEntries(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {

        log.info("📋 Ledger query from {} to {} (page: {}, size: {})", startDate, endDate, page, size);
        Page<StorageUsageLedgerDTO> entries = mediaService.getLedgerEntriesBetweenDates(startDate, endDate, page, size);
        log.info("✅ Returned {} ledger entries (page {}/{})", entries.getNumberOfElements(), page, entries.getTotalPages());
        return ResponseEntity.ok(entries);
    }
}
