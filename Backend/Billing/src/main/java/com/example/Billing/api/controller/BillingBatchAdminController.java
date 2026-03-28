package com.example.Billing.api.controller;

import com.example.Billing.application.service.InvoiceGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/jobs")
@RequiredArgsConstructor
public class BillingBatchAdminController {

    private final InvoiceGenerationService invoiceGenerationService;

    /**
     * Manually trigger monthly invoice generation job
     * Used for testing and on-demand execution
     */
    @PostMapping("/monthly/run")
    @Operation(summary = "Run monthly billing job")
    public ResponseEntity<Map<String, Object>> run() {
        log.info("📊 Manual trigger: Monthly billing job");

        try {
            InvoiceGenerationService.InvoiceGenerationResult result =
                invoiceGenerationService.processMonthlyInvoiceGeneration();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("generatedCount", result.getGeneratedCount());
            response.put("failureCount", result.getFailureCount());
            response.put("timestamp", result.getGeneratedAt());
            response.put("billingPeriodStart", result.getBillingPeriodStart());
            response.put("billingPeriodEnd", result.getBillingPeriodEnd());

            log.info("✅ Monthly billing job completed: {} generated, {} failed",
                result.getGeneratedCount(), result.getFailureCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Billing job failed", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("timestamp", Instant.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * TEST ENDPOINT: Manually trigger current month invoice generation
     * Used for testing with recent file uploads (same month)
     */
    @PostMapping("/monthly/run/current")
    @Operation(summary = "Run current month billing job (TEST)")
    public ResponseEntity<Map<String, Object>> runCurrentMonth() {
        log.info("📊 Manual trigger: Current month billing job (TEST)");

        try {
            InvoiceGenerationService.InvoiceGenerationResult result =
                invoiceGenerationService.processCurrentMonthInvoiceGeneration();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("generatedCount", result.getGeneratedCount());
            response.put("failureCount", result.getFailureCount());
            response.put("timestamp", result.getGeneratedAt());
            response.put("billingPeriodStart", result.getBillingPeriodStart());
            response.put("billingPeriodEnd", result.getBillingPeriodEnd());
            response.put("note", "This generates invoices for CURRENT month (testing purposes only)");

            log.info("✅ Current month billing job completed: {} generated, {} failed",
                result.getGeneratedCount(), result.getFailureCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Current month billing job failed", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("timestamp", Instant.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get billing job execution status
     */
    @GetMapping("/{executionId}")
    @Operation(summary = "Get billing job execution status")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String executionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("executionId", executionId);
        response.put("status", "COMPLETED");
        response.put("createdTime", Instant.now());

        return ResponseEntity.ok(response);
    }
}

