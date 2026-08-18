package com.example.Billing.api.controller;

import com.example.Billing.application.service.InvoiceGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/jobs")
@RequiredArgsConstructor
public class BillingBatchAdminController {

    private final InvoiceGenerationService invoiceGenerationService;

    @PostMapping("/monthly/run")
    @Operation(summary = "Run monthly billing job")
    public ResponseEntity<InvoiceGenerationService.InvoiceGenerationResult> run() {
        log.info("  Manual trigger: Monthly billing job");
        InvoiceGenerationService.InvoiceGenerationResult result = invoiceGenerationService.processMonthlyInvoiceGeneration();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/monthly/run/current")
    @Operation(summary = "Run current month billing job (TEST)")
    public ResponseEntity<InvoiceGenerationService.InvoiceGenerationResult> runCurrentMonth() {
        log.info("  Manual trigger: Current month billing job (TEST)");
        InvoiceGenerationService.InvoiceGenerationResult result = invoiceGenerationService.processCurrentMonthInvoiceGeneration();
        return ResponseEntity.ok(result);
    }

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