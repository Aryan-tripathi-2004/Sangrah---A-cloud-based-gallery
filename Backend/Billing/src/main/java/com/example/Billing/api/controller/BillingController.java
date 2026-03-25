package com.example.Billing.api.controller;

import com.example.Billing.api.dto.response.CostEstimateDTO;
import com.example.Billing.api.dto.response.InvoiceDTO;
import com.example.Billing.application.service.BillingService;
import com.example.Billing.application.service.CostEstimationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Billing and Invoice Management")
public class BillingController {

    private final BillingService billingService;
    private final CostEstimationService costEstimationService;

    @Value("${stripe.publishable.key:}")
    private String stripePublishableKey;

    /**
     * Get list of invoices for current user
     */
    @GetMapping("/invoices")
    @Operation(summary = "List invoices", description = "Get all invoices for the current user")
    public ResponseEntity<Page<InvoiceDTO>> getInvoices(
        HttpServletRequest request,
        Pageable pageable
    ) {
        String userId = request.getHeader("X-User-Id");
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
        HttpServletRequest request
    ) {
        String userId = request.getHeader("X-User-Id");
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
            HttpServletRequest request
    ) {
        String userId = request.getHeader("X-User-Id");
        log.info("📥 Downloading PDF for invoice {} for user: {}", invoiceId, userId);

        try {
            // Verify invoice ownership
            InvoiceDTO invoice = billingService.getInvoice(userId, invoiceId);

            // Get PDF content (would be stored in invoice document)
            // For now, we'll need to modify BillingService to return the PDF
            byte[] pdfContent = billingService.getInvoicePDF(userId, invoiceId);

            if (pdfContent == null || pdfContent.length == 0) {
                log.warn("⚠️ PDF not found for invoice: {}", invoiceId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(pdfContent.length);
            headers.setContentDispositionFormData("attachment", "invoice_" + invoiceId + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);

        } catch (Exception e) {
            log.error("❌ Error downloading PDF for invoice {}: {}", invoiceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Get real-time cost estimate for current month
     */
    @GetMapping("/cost-estimate")
    @Operation(summary = "Get cost estimate", description = "Get estimated cost for current month")
    public ResponseEntity<CostEstimateDTO> getCostEstimate(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        log.info("💰 Getting cost estimate for user: {}", userId);

        return ResponseEntity.ok(costEstimationService.estimateCurrentMonthCost(userId));
    }

    /**
     * Get billing dashboard metrics
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard metrics", description = "Get all dashboard metrics for billing")
    public ResponseEntity<?> getDashboard(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        log.info("📊 Getting dashboard for user: {}", userId);

        // Return cost estimate as dashboard data for now
        CostEstimateDTO estimate = costEstimationService.estimateCurrentMonthCost(userId);

        // TODO: Add more metrics (total paid, outstanding balance, etc.)
        return ResponseEntity.ok(estimate);
    }

    /**
     * Initiate payment for an invoice (Stripe checkout)
     */
    @PostMapping("/invoices/{invoiceId}/pay")
    @Operation(summary = "Pay invoice", description = "Initiate payment for an invoice via Stripe")
    public ResponseEntity<?> payInvoice(
        @PathVariable String invoiceId,
        HttpServletRequest request
    ) {
        String userId = request.getHeader("X-User-Id");
        log.info("💳 Initiating payment for invoice {} for user: {}", invoiceId, userId);

        // Get invoice to verify ownership
        InvoiceDTO invoice = billingService.getInvoice(userId, invoiceId);

        // TODO: Create Stripe Checkout session
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(new ErrorResponse("Stripe integration coming soon"));
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if Billing service is healthy")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(new HealthResponse("Billing service is running"));
    }

    /**
     * Get Stripe configuration (publishable key for frontend)
     */
    @GetMapping("/config")
    @Operation(summary = "Get billing config", description = "Get Stripe publishable key and other config")
    public ResponseEntity<StripeConfigResponse> getStripeConfig() {
        log.info("📋 Getting Stripe configuration");

        boolean stripeConfigured = stripePublishableKey != null && !stripePublishableKey.isEmpty();

        return ResponseEntity.ok(StripeConfigResponse.builder()
            .stripePublishableKey(stripeConfigured ? stripePublishableKey : null)
            .stripeConfigured(stripeConfigured)
            .message(stripeConfigured ? "Stripe is configured" : "Stripe not configured - add STRIPE_PUBLISHABLE_KEY")
            .build());
    }

    /**
     * Get payment history for current user
     */
    @GetMapping("/payments")
    @Operation(summary = "Get payment history", description = "Get all payments for the current user")
    public ResponseEntity<?> getPaymentHistory(
        HttpServletRequest request,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        String userId = request.getHeader("X-User-Id");
        log.info("💳 Getting payment history for user: {}", userId);

        try {
            var payments = billingService.getPaymentHistory(userId, page, size);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("❌ Error getting payment history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to load payment history: " + e.getMessage()));
        }
    }

    // Helper classes
    @lombok.Data
    @lombok.AllArgsConstructor
    static class ErrorResponse {
        private String error;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    static class HealthResponse {
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class StripeConfigResponse {
        private String stripePublishableKey;
        private Boolean stripeConfigured;
        private String message;
    }
}

