package com.example.Media.api.controller;

import com.example.Media.api.dto.response.StorageUsageLedgerResponse;
import com.example.Media.api.dto.response.StorageUsageResponse;
import com.example.Media.application.service.interfaces.IMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Internal-only endpoints for inter-service communication with the Media microservice.
 * Gateway blocks external access to /internal/**.
 */
@Slf4j
@RestController
@RequestMapping("/internal/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Internal Media", description = "Internal endpoints for billing and inter-service coordination")
public class InternalMediaController {

    private final IMediaService mediaService;

    @GetMapping("/usage")
    @Operation(summary = "Get user storage usage")
    public ResponseEntity<StorageUsageResponse> getStorageUsage(@RequestParam String userId) {
        log.info("📊 [INTERNAL] Fetching storage usage for user: {}", userId);
        return ResponseEntity.ok(mediaService.getStorageUsage(userId));
    }

    @GetMapping("/ledger/user")
    @Operation(summary = "Get user storage ledger entries")
    public ResponseEntity<List<StorageUsageLedgerResponse>> getUserStorageLedger(
            @RequestParam String userId,
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {
        log.info("📊 [INTERNAL] Querying user ledger | user: {} | period: {} to {}", userId, startDate, endDate);
        return ResponseEntity.ok(mediaService.getStorageLedger(userId, startDate, endDate));
    }

    @GetMapping("/ledger")
    @Operation(summary = "Get paginated storage ledger entries")
    public ResponseEntity<Page<StorageUsageLedgerResponse>> getPaginatedStorageLedger(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {
        log.info("📊 [INTERNAL] Querying paginated ledger | period: {} to {} | page: {} size: {}", startDate, endDate, page, size);
        return ResponseEntity.ok(mediaService.getLedgerEntriesBetweenDates(startDate, endDate, page, size));
    }
}
