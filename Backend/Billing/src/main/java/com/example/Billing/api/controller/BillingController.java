package com.example.Billing.api.controller;

import com.example.Billing.api.dto.response.CostEstimateDTO;
import com.example.Billing.api.dto.response.HealthResponse;
import com.example.Billing.api.dto.response.InvoiceDTO;
import com.example.Billing.api.dto.response.PaymentDTO;
import com.example.Billing.api.dto.response.StripeConfigResponse;
import com.example.Billing.api.resolver.CurrentUserId;
import com.example.Billing.application.service.interfaces.IBillingService;
import com.example.Billing.application.service.interfaces.ICostEstimationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Billing and Invoice Management")
public class BillingController {

    private final IBillingService billingService;
    private final ICostEstimationService costEstimationService;

    /**
     * Get list of invoices for current user
     */
    @GetMapping("/invoices")
    @Operation(summary = "List invoices", description = "Get all invoices for the current user")
    public ResponseEntity<Page<InvoiceDTO>> getInvoices(
        @CurrentUserId String userId,
        Pageable pageable
    ) {
        log.info("📋 Getting invoices for user: {}", userId);
        return ResponseEntity.ok(billingService.getInvoiceHistory(userId, pageable));
    }

    /**
     * Get single invoice details
     */
    @GetMapping("/invoices/{invoiceId}")
    @Operation(summary = "Get invoice details", description = "Get details of a specific invoice")
    public ResponseEntity<InvoiceDTO> getInvoice(
        @PathVariable String invoiceId,
        @CurrentUserId String userId
    ) {
        log.info("📄 Getting invoice {} for user: {}", invoiceId, userId);
        return ResponseEntity.ok(billingService.getInvoice(userId, invoiceId));
    }

    /**
     * Download invoice PDF
     */
    @GetMapping("/invoices/{invoiceId}/pdf")
    @Operation(summary = "Download invoice PDF", description = "Download PDF for a specific invoice")
    public ResponseEntity<byte[]> downloadInvoicePDF(
            @PathVariable String invoiceId,
            @CurrentUserId String userId
    ) {
        log.info("📥 Downloading PDF for invoice {} for user: {}", invoiceId, userId);

        byte[] pdfContent = billingService.getInvoicePDF(userId, invoiceId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdfContent.length);
        headers.setContentDispositionFormData("attachment", "invoice_" + invoiceId + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }

    /**
     * Get real-time cost estimate for current month
     */
    @GetMapping("/cost-estimate")
    @Operation(summary = "Get cost estimate", description = "Get estimated cost for current month")
    public ResponseEntity<CostEstimateDTO> getCostEstimate(@CurrentUserId String userId) {
        log.info("💰 Getting cost estimate for user: {}", userId);
        return ResponseEntity.ok(costEstimationService.estimateCurrentMonthCost(userId));
    }

    /**
     * Get billing dashboard metrics
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard metrics", description = "Get all dashboard metrics for billing")
    public ResponseEntity<CostEstimateDTO> getDashboard(@CurrentUserId String userId) {
        log.info("📊 Getting dashboard for user: {}", userId);
        // Return cost estimate as dashboard data for now
        // TODO: Add more metrics (total paid, outstanding balance, etc.)
        return ResponseEntity.ok(costEstimationService.estimateCurrentMonthCost(userId));
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if Billing service is healthy")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(billingService.getHealth());
    }

    /**
     * Get Stripe configuration (publishable key for frontend)
     */
    @GetMapping("/config")
    @Operation(summary = "Get billing config", description = "Get Stripe publishable key and other config")
    public ResponseEntity<StripeConfigResponse> getStripeConfig() {
        log.info("📋 Getting Stripe configuration");
        return ResponseEntity.ok(billingService.getStripeConfig());
    }

    /**
     * Get payment history for current user
     */
    @GetMapping("/payments")
    @Operation(summary = "Get payment history", description = "Get all payments for the current user")
    public ResponseEntity<List<PaymentDTO>> getPaymentHistory(
        @CurrentUserId String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        log.info("💳 Getting payment history for user: {}", userId);
        return ResponseEntity.ok(billingService.getPaymentHistory(userId, page, size));
    }
}
