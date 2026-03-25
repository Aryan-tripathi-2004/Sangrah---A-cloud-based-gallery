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

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Controller for storage usage endpoints.
 * Note: JWT validation happens at API Gateway level.
 * This service simply reads the X-User-Id header provided by the gateway.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Tag(name = "Storage Usage", description = "APIs for storage usage information")
public class StorageUsageController {

    private final StorageUsageLedgerRepository storageUsageLedgerRepository;

    /**
     * Get current user's storage usage summary.
     * Returns the total bytes used and limit.
     * Authentication: API Gateway validates JWT and provides X-User-Id header
     */
    @GetMapping("/storage-usage")
    @Operation(
        summary = "Get storage usage for current user",
        description = "Returns storage usage metrics including used bytes and limit"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> getStorageUsage(HttpServletRequest request) {
        try {
            // Extract userId from X-User-Id header (added by API Gateway after JWT validation)
            String userId = request.getHeader("X-User-Id");

            if (userId == null || userId.isBlank()) {
                log.warn("❌ No X-User-Id header found - gateway validation may have failed");
                return ResponseEntity.ok(Map.of(
                    "usedBytes", 0L,
                    "limitBytes", 5_368_709_120L
                ));
            }

            log.info("📊 Fetching storage usage for user: {}", userId);

            // Get storage usage records for this user
            List<StorageUsageLedgerDocument> usageRecords = storageUsageLedgerRepository.findByUserId(userId);

            // Calculate total bytes used
            long totalBytesUsed = usageRecords.stream()
                .mapToLong(StorageUsageLedgerDocument::getSizeBytes)
                .sum();

            // Default storage limit: 5 GB
            long storageLimitBytes = 5_368_709_120L;

            log.info("✅ Storage usage retrieved - Used: {} bytes, Limit: {} bytes",
                totalBytesUsed, storageLimitBytes);

            return ResponseEntity.ok(Map.of(
                "usedBytes", totalBytesUsed,
                "limitBytes", storageLimitBytes
            ));

        } catch (Exception e) {
            log.error("❌ Error fetching storage usage: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch storage usage",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get detailed storage usage records for current user.
     */
    @GetMapping("/storage-usage/details")
    @Operation(
        summary = "Get detailed storage usage records",
        description = "Returns detailed storage usage ledger entries for the current user"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getStorageUsageDetails(HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");

            if (userId == null || userId.isBlank()) {
                log.warn("❌ No X-User-Id header found");
                return ResponseEntity.ok(List.of());
            }

            log.info("📊 Fetching storage usage details for user: {}", userId);

            List<StorageUsageLedgerDocument> usageRecords = storageUsageLedgerRepository.findByUserId(userId);
            log.info("✅ Found {} storage usage records for user: {}", usageRecords.size(), userId);

            return ResponseEntity.ok(usageRecords);

        } catch (Exception e) {
            log.error("❌ Error fetching storage usage details: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch storage usage details",
                "message", e.getMessage()
            ));
        }
    }
}
