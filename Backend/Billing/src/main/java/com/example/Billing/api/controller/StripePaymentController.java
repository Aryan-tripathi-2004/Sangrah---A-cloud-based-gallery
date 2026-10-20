package com.example.Billing.api.controller;

import com.example.Billing.api.dto.response.CheckoutSessionResponse;
import com.example.Billing.api.dto.response.PaymentStatusResponse;
import com.example.Billing.api.resolver.CurrentUserId;
import com.example.Billing.application.service.interfaces.IStripePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/payments")
@RequiredArgsConstructor
@Tag(name = "Stripe Payments", description = "Payment processing and Stripe integration")
public class StripePaymentController {

    private final IStripePaymentService stripePaymentService;

    /**
     * Create a Stripe checkout session (payment intent) for an invoice
     */
    @PostMapping("/{invoiceId}/checkout")
    @Operation(summary = "Create payment intent for invoice")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @PathVariable String invoiceId,
            @CurrentUserId String userId) throws Exception {
        log.info("💳 Creating checkout session for invoice: {}", invoiceId);
        return ResponseEntity.ok(stripePaymentService.createCheckoutSession(userId, invoiceId));
    }

    /**
     * Get the payment status for a specific invoice
     */
    @GetMapping("/{invoiceId}/status")
    @Operation(summary = "Get payment status for invoice")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable String invoiceId,
            @CurrentUserId String userId) {
        log.info("📊 Getting payment status for invoice: {}", invoiceId);
        return ResponseEntity.ok(stripePaymentService.getPaymentStatus(userId, invoiceId));
    }

    /**
     * Sync payment status from Stripe and update the database
     */
    @PostMapping("/{invoiceId}/sync")
    @Operation(summary = "Sync payment status from Stripe and update database")
    public ResponseEntity<PaymentStatusResponse> syncPaymentStatus(
            @PathVariable String invoiceId,
            @CurrentUserId String userId) {
        log.info("🔄 Syncing payment status with Stripe for invoice: {}", invoiceId);
        return ResponseEntity.ok(stripePaymentService.syncPaymentStatus(invoiceId, userId));
    }
}
