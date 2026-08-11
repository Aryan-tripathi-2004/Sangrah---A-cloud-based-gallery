package com.example.Billing.api.controller;

import com.example.Billing.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Billing.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.Billing.api.resolver.CurrentUserId;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Tag(name = "Storage Usage", description = "APIs for storage usage information")
public class StorageUsageController {

    private final StorageUsageLedgerRepository storageUsageLedgerRepository;

    @GetMapping("/storage-usage")
    @Operation(summary = "Get storage usage for current user")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> getStorageUsage(@CurrentUserId String userId) {
        log.info("  Fetching storage usage for user: {}", userId);
        List<StorageUsageLedgerDocument> usageRecords = storageUsageLedgerRepository.findByUserId(userId);
        
        long totalBytesUsed = usageRecords.stream()
            .mapToLong(StorageUsageLedgerDocument::getSizeBytes)
            .sum();
        long storageLimitBytes = 5_368_709_120L; // 5 GB
        
        return ResponseEntity.ok(Map.of(
            "usedBytes", totalBytesUsed,
            "limitBytes", storageLimitBytes
        ));
    }

    @GetMapping("/storage-usage/details")
    @Operation(summary = "Get detailed storage usage records")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<StorageUsageLedgerDocument>> getStorageUsageDetails(@CurrentUserId String userId) {
        log.info("  Fetching storage usage details for user: {}", userId);
        List<StorageUsageLedgerDocument> usageRecords = storageUsageLedgerRepository.findByUserId(userId);
        return ResponseEntity.ok(usageRecords);
    }
}