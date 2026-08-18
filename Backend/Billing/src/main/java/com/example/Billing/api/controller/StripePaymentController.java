package com.example.Billing.api.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.example.Billing.application.service.interfaces.IStripePaymentService;
import com.example.Billing.infrastructure.persistence.repository.InvoiceRepository;
import com.example.Billing.infrastructure.persistence.document.InvoiceDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Billing.api.resolver.CurrentUserId;
import com.example.Billing.shared.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for Stripe payment processing
 * Endpoints for creating payment intents, handling webhooks, and managing payment methods
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/billing/payments")
@RequiredArgsConstructor
@Tag(name = "Stripe Payments", description = "Payment processing and Stripe integration")
public class StripePaymentController {

    private final IStripePaymentService stripePaymentService;
    private final InvoiceRepository invoiceRepository;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    /**
     * Create Stripe payment intent for an invoice
     * POST /api/v1/billing/payments/{invoiceId}/checkout
     */
    @PostMapping("/{invoiceId}/checkout")
    @Operation(summary = "Create payment intent for invoice")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @PathVariable String invoiceId,
            @CurrentUserId String userId) throws Exception {
        log.info("💳 Creating checkout session for invoice: {}", invoiceId);

        // Get invoice
        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        // Verify user owns this invoice
        if (!invoice.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Create payment intent
        Double amount = invoice.getCharges().getTotalAmount();
        String clientSecret = stripePaymentService.createPaymentIntent(
            userId, invoiceId, amount
        );

        log.info("✅ Checkout session created for invoice: {}", invoiceId);

        return ResponseEntity.ok(CheckoutSessionResponse.builder()
            .invoiceId(invoiceId)
            .clientSecret(clientSecret)
            .amount(amount)
            .currency("usd")
            .build());
    }

    /**
     * Handle Stripe webhook events (payment success/failure)
     * POST /api/v1/billing/payments/webhook
     * CRITICAL: Must validate webhook signature to prevent CSRF attacks
     */
    @PostMapping("/webhook")
    @Operation(summary = "Handle Stripe webhook events")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sig) {
        log.info("🔔 Received Stripe webhook");

        try {
            if (webhookSecret == null || webhookSecret.isEmpty()) {
                log.warn("⚠️ Webhook secret not configured - webhook validation skipped");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Validate webhook signature
            Event event = Webhook.constructEvent(payload, sig, webhookSecret);

            // Extract event data
            StripeObject dataObject = event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

            if (dataObject == null) {
                log.warn("⚠️ Could not extract event data");
                return ResponseEntity.ok().build();
            }

            // Handle different event types
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded((PaymentIntent) dataObject);
                    break;

                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed((PaymentIntent) dataObject);
                    break;

                default:
                    log.debug("⏭️ Ignoring event type: {}", event.getType());
            }

            return ResponseEntity.ok().build();

        } catch (SignatureVerificationException e) {
            log.error("❌ Invalid webhook signature - rejecting webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        } catch (Exception e) {
            log.error("❌ Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get payment status for an invoice
     */
    @GetMapping("/{invoiceId}/status")
    @Operation(summary = "Get payment status for invoice")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable String invoiceId) {
        log.info("📊 Getting payment status for invoice: {}", invoiceId);

        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        return ResponseEntity.ok(PaymentStatusResponse.builder()
            .invoiceId(invoiceId)
            .status(invoice.getStatus().name())  // PENDING, PAID, OVERDUE
            .paidDate(invoice.getPaidDate())
            .build());
    }

    /**
     * Sync payment status with Stripe and update database
     * Called by frontend after payment confirmation to sync status
     * POST /api/v1/billing/payments/{invoiceId}/sync
     */
    @PostMapping("/{invoiceId}/sync")
    @Operation(summary = "Sync payment status from Stripe and update database")
    public ResponseEntity<PaymentStatusResponse> syncPaymentStatus(
            @PathVariable String invoiceId) throws Exception {
        log.info("🔄 Syncing payment status with Stripe for invoice: {}", invoiceId);

        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        // If no payment intent ID, return current status
        if (invoice.getPaymentIntentId() == null || invoice.getPaymentIntentId().isEmpty()) {
            log.warn("⚠️ No payment intent ID for invoice: {}", invoiceId);
            return ResponseEntity.ok(PaymentStatusResponse.builder()
                .invoiceId(invoiceId)
                .status(invoice.getStatus().name())
                .paidDate(invoice.getPaidDate())
                .build());
        }

        // Call Stripe service to sync status
        stripePaymentService.syncPaymentStatusWithStripe(invoiceId, invoice.getPaymentIntentId());

        // Re-fetch invoice to get updated status
        invoice = invoiceRepository.findByInvoiceId(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        log.info("✅ Payment status synced: {}", invoice.getStatus());

        return ResponseEntity.ok(PaymentStatusResponse.builder()
            .invoiceId(invoiceId)
            .status(invoice.getStatus().name())
            .paidDate(invoice.getPaidDate())
            .build());
    }

    /**
     * Private helper: Handle successful payment
     */
    private void handlePaymentIntentSucceeded(PaymentIntent intent) {
        log.info("✅ Processing payment_intent.succeeded: {}", intent.getId());
        stripePaymentService.handlePaymentSuccess(intent.getId());
    }

    /**
     * Private helper: Handle failed payment
     */
    private void handlePaymentIntentFailed(PaymentIntent intent) {
        String failureReason = "Unknown error";
        if (intent.getLastPaymentError() != null) {
            failureReason = intent.getLastPaymentError().getMessage();
        }
        log.info("❌ Processing payment_intent.payment_failed: {} - {}", intent.getId(), failureReason);
        stripePaymentService.handlePaymentFailed(intent.getId(), failureReason);
    }

    /**
     * Response DTOs
     */
    @Builder
    public record CheckoutSessionResponse(
        String invoiceId,
        String clientSecret,
        Double amount,
        String currency
    ) {}

    @Builder
    public record PaymentStatusResponse(
        String invoiceId,
        String status,
        Instant paidDate
    ) {}

    @Builder
    public record ErrorResponse(
        String error,
        String message,
        Instant timestamp
    ) {}
}
