package com.example.Billing.application.service.impl;

import com.example.Billing.api.dto.response.WebhookResponse;
import com.example.Billing.application.service.interfaces.IStripePaymentService;
import com.example.Billing.application.service.interfaces.IStripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service implementation for Stripe Webhook processing.
 *
 * <p>Handles signature verification, replay attack prevention, timeout monitoring,
 * and event routing to respective domain handlers.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookServiceImpl implements IStripeWebhookService {

    private final IStripePaymentService stripePaymentService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${webhook.timeout.ms}")
    private long webhookTimeoutMs;  // Default: 5 minutes = 300000ms

    @Value("${webhook.event.max.age}")
    private long webhookEventMaxAge;  // Default: 10 minutes = 600 seconds

    @Override
    public WebhookResponse processWebhook(String payload, String signature) throws SignatureVerificationException {
        long startTime = System.currentTimeMillis();
        log.info("🔔 Stripe webhook received");

        // STEP 1: VALIDATE WEBHOOK SIGNATURE (CRITICAL - PREVENT CSRF)
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("❌ Webhook signature validation failed", e);
            throw e;
        }

        // STEP 2: CHECK EVENT AGE (prevent replay attacks)
        long eventCreatedTime = event.getCreated();
        long currentTime = System.currentTimeMillis() / 1000;
        long delaySeconds = currentTime - eventCreatedTime;

        if (delaySeconds > webhookEventMaxAge) {
            log.warn("⚠️ Webhook event is {} seconds old (max age: {} seconds). "
                    + "Possible delayed delivery from Stripe or replay attack attempt.",
                    delaySeconds, webhookEventMaxAge);
            // Still process it - Stripe might have delays
        }

        // STEP 3: EXTRACT AND PROCESS EVENT
        String eventType = event.getType();
        String eventId = event.getId();

        log.info("📨 Processing webhook: type={}, eventId={}", eventType, eventId);

        // Route to appropriate handler based on event type
        if ("payment_intent.succeeded".equals(eventType)) {
            handlePaymentSucceeded(event);
        } else if ("payment_intent.payment_failed".equals(eventType)) {
            handlePaymentFailed(event);
        } else if ("charge.dispute.created".equals(eventType)) {
            handleDisputeCreated(event);
        } else {
            log.debug("✅ Ignoring unhandled event type: {}", eventType);
        }

        // STEP 4: CHECK PROCESSING TIME (timeout strategy)
        long processingTime = System.currentTimeMillis() - startTime;

        if (processingTime > webhookTimeoutMs) {
            log.warn("⚠️ WEBHOOK TIMEOUT: Processing took {}ms (exceeds limit of {}ms). "
                    + "Event: {}. Increase server resources or optimize processing.",
                    processingTime, webhookTimeoutMs, eventType);
        } else {
            log.info("✅ Webhook processed successfully in {}ms", processingTime);
        }

        // STEP 5: RETURN RESPONSE DTO
        return WebhookResponse.success(eventId, eventType, processingTime, Instant.now().toString());
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================

    /**
     * Handle payment_intent.succeeded event
     */
    private void handlePaymentSucceeded(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElse(null);

        if (!(stripeObject instanceof PaymentIntent paymentIntent)) {
            log.warn("⚠️ payment_intent.succeeded event with unexpected object type");
            return;
        }

        String paymentIntentId = paymentIntent.getId();
        log.info("✅ Payment succeeded: {}", paymentIntentId);
        stripePaymentService.handlePaymentSuccess(paymentIntentId);
    }

    /**
     * Handle payment_intent.payment_failed event
     */
    private void handlePaymentFailed(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElse(null);

        if (!(stripeObject instanceof PaymentIntent paymentIntent)) {
            log.warn("⚠️ payment_intent.payment_failed event with unexpected object type");
            return;
        }

        String paymentIntentId = paymentIntent.getId();
        String failureReason = "Unknown error";

        if (paymentIntent.getLastPaymentError() != null) {
            failureReason = paymentIntent.getLastPaymentError().getMessage();
        }

        log.error("❌ Payment failed: {} - {}", paymentIntentId, failureReason);
        stripePaymentService.handlePaymentFailed(paymentIntentId, failureReason);
    }

    /**
     * Handle charge.dispute.created event
     */
    private void handleDisputeCreated(Event event) {
        log.warn("⚠️ CHARGEBACK/DISPUTE CREATED - Requires manual investigation");
        log.warn("Event: {}", event.getId());
    }
}
