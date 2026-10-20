package com.example.Billing.api.controller;

import com.example.Billing.api.dto.response.InvoiceGenerationResultResponse;
import com.example.Billing.api.dto.response.JobStatusResponse;
import com.example.Billing.application.service.interfaces.IInvoiceGenerationService;
import com.example.Billing.application.service.interfaces.IInvoiceJobService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/jobs")
@RequiredArgsConstructor
public class BillingBatchAdminController {

    private final IInvoiceGenerationService invoiceGenerationService;
    private final IInvoiceJobService invoiceJobService;

    /**
     * Manually trigger the monthly billing invoice generation job
     */
    @PostMapping("/monthly-invoices")
    @Operation(summary = "Run monthly billing job")
    public ResponseEntity<InvoiceGenerationResultResponse> runMonthly() {
        log.info("  Manual trigger: Monthly billing job");
        return ResponseEntity.ok(invoiceGenerationService.processMonthlyInvoiceGeneration());
    }

    /**
     * Manually trigger the current month billing invoice generation job (TEST only)
     */
    @PostMapping("/monthly-invoices/current")
    @Operation(summary = "Run current month billing job (TEST)")
    public ResponseEntity<InvoiceGenerationResultResponse> runCurrentMonth() {
        log.info("  Manual trigger: Current month billing job (TEST)");
        return ResponseEntity.ok(invoiceGenerationService.processCurrentMonthInvoiceGeneration());
    }

    /**
     * Get billing job execution status by execution ID
     */
    @GetMapping("/{executionId}")
    @Operation(summary = "Get billing job execution status")
    public ResponseEntity<JobStatusResponse> status(@PathVariable String executionId) {
        return ResponseEntity.ok(invoiceJobService.getJobStatus(executionId));
    }
}