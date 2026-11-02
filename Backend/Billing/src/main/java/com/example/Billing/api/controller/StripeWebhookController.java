package com.example.Billing.api.controller;

import com.example.Billing.api.dto.response.WebhookResponse;
import com.example.Billing.application.service.interfaces.IStripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing/webhooks")
@RequiredArgsConstructor
@Tag(name = "Stripe Webhooks", description = "Real-time payment status updates from Stripe")
public class StripeWebhookController {

    private final IStripeWebhookService stripeWebhookService;

    @PostMapping("/stripe")
    @Operation(summary = "Handle Stripe webhook events")
    public ResponseEntity<WebhookResponse> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) throws SignatureVerificationException {
            
        WebhookResponse response = stripeWebhookService.processWebhook(payload, signature);
        return ResponseEntity.ok(response);
    }
}
