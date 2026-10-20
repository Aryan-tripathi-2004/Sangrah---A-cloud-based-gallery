package com.example.Billing.api.controller;

import com.example.Billing.api.dto.response.StorageUsageLedgerDTO;
import com.example.Billing.api.dto.response.StorageUsageSummaryResponse;
import com.example.Billing.api.resolver.CurrentUserId;
import com.example.Billing.application.service.interfaces.IBillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Tag(name = "Storage Usage", description = "APIs for storage usage information")
public class StorageUsageController {

    private final IBillingService billingService;

    /**
     * Retrieves the summarised storage usage (total bytes used and storage limit)
     * for the currently authenticated user.
     */
    @GetMapping("/storage-usage")
    @Operation(summary = "Get storage usage for current user")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<StorageUsageSummaryResponse> getStorageUsage(@CurrentUserId String userId) {
        log.info("  Fetching storage usage for user: {}", userId);
        return ResponseEntity.ok(billingService.getStorageUsageSummary(userId));
    }

    /**
     * Retrieves the detailed per-file storage usage ledger records for the
     * currently authenticated user.
     */
    @GetMapping("/storage-usage/details")
    @Operation(summary = "Get detailed storage usage records")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<StorageUsageLedgerDTO>> getStorageUsageDetails(@CurrentUserId String userId) {
        log.info("  Fetching storage usage details for user: {}", userId);
        return ResponseEntity.ok(billingService.getStorageUsageDetails(userId));
    }
}