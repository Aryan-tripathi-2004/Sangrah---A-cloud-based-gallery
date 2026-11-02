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

    @PostMapping("/monthly-invoices")
    @Operation(summary = "Run monthly billing job")
    public ResponseEntity<InvoiceGenerationResultResponse> runMonthly(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        log.info("Manual trigger: Monthly billing job");
        return ResponseEntity.ok(invoiceGenerationService.triggerMonthlyInvoiceGenerationForAdmin(role));
    }

    @PostMapping("/monthly-invoices/current")
    @Operation(summary = "Run current month billing job (TEST)")
    public ResponseEntity<InvoiceGenerationResultResponse> runCurrentMonth(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        log.info("Manual trigger: Current month billing job (TEST)");
        return ResponseEntity.ok(invoiceGenerationService.triggerCurrentMonthInvoiceGenerationForAdmin(role));
    }

    @GetMapping("/{executionId}")
    @Operation(summary = "Get billing job execution status")
    public ResponseEntity<JobStatusResponse> status(
            @PathVariable String executionId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return ResponseEntity.ok(invoiceJobService.getAuthorizedJobStatus(executionId, role));
    }
}
