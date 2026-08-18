package com.example.Billing.application.service.impl;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.example.Billing.application.service.interfaces.IStripePaymentService;
import com.example.Billing.infrastructure.persistence.document.InvoiceDocument;
import com.example.Billing.infrastructure.persistence.document.PaymentDocument;
import com.example.Billing.infrastructure.persistence.document.UserBillingSettingsDocument;
import com.example.Billing.infrastructure.persistence.repository.InvoiceRepository;
import com.example.Billing.infrastructure.persistence.repository.PaymentRepository;
import com.example.Billing.infrastructure.persistence.repository.UserBillingSettingsRepository;
import com.example.Billing.infrastructure.event.InvoicePaidEvent;
import com.example.Billing.infrastructure.event.PaymentFailedEvent;
import com.example.Billing.shared.enums.InvoiceStatus;
import com.example.Billing.shared.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling Stripe payment processing
 * Manages:
 * - Stripe customer creation
 * - Payment intent creation
 * - Webhook handling
 * - Auto-retry logic for failed payments
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentServiceImpl implements IStripePaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final UserBillingSettingsRepository settingsRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    /**
     * Initialize Stripe API key on startup
     */
    @PostConstruct
    public void initializeStripe() {
        if (stripeApiKey != null && !stripeApiKey.isEmpty()) {
            Stripe.apiKey = stripeApiKey;
            log.info("💳 Stripe API initialized");
        } else {
            log.warn("⚠️ WARNING: Stripe API key not configured. Payment processing disabled.");
        }
    }

    /**
     * Create a Stripe payment intent for an invoice
     * @param userId User ID
     * @param invoiceId Invoice ID
     * @param amount Amount in dollars
     * @return Client secret for Stripe payment submission
     */
    public String createPaymentIntent(String userId, String invoiceId, Double amount)
            throws StripeException {
        log.info("💳 Creating payment intent for invoice: {}, amount: ${}", invoiceId, amount);

        // Get or create Stripe customer
        UserBillingSettingsDocument settings = settingsRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultUserSettings(userId));

        if (settings.getStripeCustomerId() == null) {
            String customerId = createStripeCustomer(userId, settings.getBilling().getEmail());
            settings.setStripeCustomerId(customerId);
            settingsRepository.save(settings);
            log.info("✅ Created new Stripe customer: {}", customerId);
        }

        // Create payment intent with automatic payment methods
        // Billing details will be collected by frontend payment element (required for India compliance)
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount((long)(amount * 100))  // Convert to cents
            .setCurrency("usd")
            .setCustomer(settings.getStripeCustomerId())
            .setDescription("Invoice " + invoiceId)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build()
            )
            .build();

        log.info("📋 Payment intent created for customer: {}, amount: {}",
            settings.getStripeCustomerId(), amount);

        PaymentIntent intent = PaymentIntent.create(params);

        // Store payment intent ID in invoice
        Optional<InvoiceDocument> invoiceOpt = invoiceRepository.findByInvoiceId(invoiceId);
        if (invoiceOpt.isPresent()) {
            InvoiceDocument invoice = invoiceOpt.get();
            invoice.setPaymentIntentId(intent.getId());
            invoiceRepository.save(invoice);
            log.info("💳 Payment intent created: {}", intent.getId());
        }

        return intent.getClientSecret();
    }

    /**
     * Handle successful payment from Stripe webhook
     */
    public void handlePaymentSuccess(String paymentIntentId) {
        log.info("💳 Processing successful payment: {}", paymentIntentId);

        try {
            // Retrieve payment intent from Stripe
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);

            // Find associated invoice
            List<InvoiceDocument> invoices = invoiceRepository.findByPaymentIntentId(paymentIntentId);
            if (invoices.isEmpty()) {
                log.warn("⚠️ No invoice found for payment intent: {}", paymentIntentId);
                return;
            }

            InvoiceDocument invoice = invoices.get(0);

            // Mark invoice as PAID
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidDate(Instant.now());
            invoice.setUpdatedAt(Instant.now());
            invoiceRepository.save(invoice);

            // Create payment record
            // Get amount from PaymentIntent (stored in cents, convert to dollars)
            Double amount = (intent.getAmount() != null) ? intent.getAmount() / 100.0 :
                           (invoice.getCharges() != null ? invoice.getCharges().getTotalAmount() : 0.0);

            PaymentDocument payment = PaymentDocument.builder()
                .userId(invoice.getUserId())
                .invoiceId(invoice.getId())
                .amount(amount)
                .status(PaymentStatus.SUCCESS)
                .stripePaymentIntentId(paymentIntentId)
                .stripeChargeId(null)  // Charge ID can be queried from Stripe if needed using paymentIntentId
                .transactionDate(Instant.now())
                .createdAt(Instant.now())
                .build();
            paymentRepository.save(payment);

            log.info("✅ Payment marked as SUCCESS for invoice: {}", invoice.getInvoiceId());

            // ✨ NEW: Publish event for listeners (notifications, PDF generation, etc.)
            eventPublisher.publishEvent(new InvoicePaidEvent(
                this,
                invoice.getInvoiceId(),
                invoice.getUserId(),
                amount
            ));
            log.debug("📢 Published InvoicePaidEvent for invoice: {}", invoice.getInvoiceId());

        } catch (StripeException e) {
            log.error("❌ Error processing payment success", e);
        }
    }

    /**
     * Handle failed payment from Stripe webhook
     */
    public void handlePaymentFailed(String paymentIntentId, String failureReason) {
        log.info("❌ Processing failed payment: {} - {}", paymentIntentId, failureReason);

        try {
            // Retrieve payment intent from Stripe
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);

            // Find associated invoice
            List<InvoiceDocument> invoices = invoiceRepository.findByPaymentIntentId(paymentIntentId);
            if (invoices.isEmpty()) {
                log.warn("⚠️ No invoice found for payment intent: {}", paymentIntentId);
                return;
            }

            InvoiceDocument invoice = invoices.get(0);

            // Create payment failure record
            // Get amount from invoice or calculate from payment intent
            Double amount = (invoice.getCharges() != null) ? invoice.getCharges().getTotalAmount() :
                           (intent.getAmount() != null ? intent.getAmount() / 100.0 : 0.0);

            PaymentDocument payment = PaymentDocument.builder()
                .userId(invoice.getUserId())
                .invoiceId(invoice.getId())
                .amount(amount)
                .status(PaymentStatus.FAILED)
                .stripePaymentIntentId(paymentIntentId)
                .failureReason(failureReason)
                .retryCount(0)
                .nextRetryDate(Instant.now().plus(Duration.ofHours(24)))
                .transactionDate(Instant.now())
                .createdAt(Instant.now())
                .build();
            paymentRepository.save(payment);

            log.info("❌ Payment marked as FAILED for invoice: {}", invoice.getInvoiceId());

            // ✨ NEW: Publish event for listeners (send failure notification email)
            eventPublisher.publishEvent(new PaymentFailedEvent(
                this,
                invoice.getId(),
                invoice.getUserId(),
                failureReason,
                0  // Initial retry count
            ));
            log.debug("📢 Published PaymentFailedEvent for invoice: {}", invoice.getInvoiceId());

        } catch (StripeException e) {
            log.error("❌ Error processing payment failure", e);
        }
    }

    /**
     * Auto-retry failed payments
     * Runs every hour to check for payments due for retry
     */
    @Scheduled(fixedRate = 3600000)  // Every hour (3600000 ms)
    public void retryFailedPayments() {
        log.info("🔄 Checking for failed payments to retry...");

        try {
            List<PaymentDocument> failedPayments = paymentRepository
                .findByStatusAndNextRetryDateLessThanEqualOrderByNextRetryDateAsc(PaymentStatus.FAILED, Instant.now());

            log.info("📊 Found {} payments due for retry", failedPayments.size());

            for (PaymentDocument payment : failedPayments) {
                if (payment.getRetryCount() >= 3) {
                    log.warn("⚠️ Payment {} exceeded max retries (3)", payment.getId());
                    continue;
                }

                try {
                    // Get the associated invoice
                    Optional<InvoiceDocument> invoiceOpt = invoiceRepository.findById(payment.getInvoiceId());
                    if (invoiceOpt.isPresent()) {
                        InvoiceDocument invoice = invoiceOpt.get();

                        // Only retry PENDING invoices
                        if (InvoiceStatus.PENDING.equals(invoice.getStatus())) {
                            log.info("🔄 Retrying payment for invoice: {}", invoice.getInvoiceId());

                            // Create new payment intent
                            String clientSecret = createPaymentIntent(
                                payment.getUserId(),
                                invoice.getInvoiceId(),
                                invoice.getCharges().getTotalAmount()
                            );

                            // Update retry metadata
                            payment.setRetryCount(payment.getRetryCount() + 1);
                            payment.setNextRetryDate(Instant.now().plus(Duration.ofHours(24)));
                            paymentRepository.save(payment);

                            log.info("✅ Queued retry #{} for payment: {}", payment.getRetryCount(), payment.getId());
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ Error retrying payment: {}", payment.getId(), e);
                }
            }

        } catch (Exception e) {
            log.error("❌ Error in auto-retry scheduler", e);
        }
    }

    /**
     * Sync payment status from Stripe and update invoice/payment records
     * Directly queries Stripe to get actual payment intent status
     * This is the fallback when webhook delivery is not available
     */
    public void syncPaymentStatusWithStripe(String invoiceId, String paymentIntentId) {
        log.info("🔄 Syncing payment status from Stripe for payment intent: {}", paymentIntentId);

        try {
            // Retrieve payment intent from Stripe
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            log.info("📊 Stripe PaymentIntent status: {}", intent.getStatus());

            Optional<InvoiceDocument> invoiceOpt = invoiceRepository.findByInvoiceId(invoiceId);
            if (invoiceOpt.isEmpty()) {
                log.warn("⚠️ Invoice not found: {}", invoiceId);
                return;
            }

            InvoiceDocument invoice = invoiceOpt.get();

            // Check if payment succeeded
            if ("succeeded".equals(intent.getStatus())) {
                log.info("✅ Payment intent succeeded! Updating invoice: {}", invoiceId);

                // Mark invoice as PAID
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setPaidDate(Instant.now());
                invoice.setUpdatedAt(Instant.now());
                invoiceRepository.save(invoice);

                // Create payment record
                Double amount = (intent.getAmount() != null) ? intent.getAmount() / 100.0 :
                               (invoice.getCharges() != null ? invoice.getCharges().getTotalAmount() : 0.0);

                // Check if payment record already exists
                Optional<PaymentDocument> existingPayment = paymentRepository.findByStripePaymentIntentId(paymentIntentId);
                if (existingPayment.isEmpty()) {
                    PaymentDocument payment = PaymentDocument.builder()
                        .userId(invoice.getUserId())
                        .invoiceId(invoice.getId())
                        .amount(amount)
                        .status(PaymentStatus.SUCCESS)
                        .stripePaymentIntentId(paymentIntentId)
                        .stripeChargeId(null)
                        .transactionDate(Instant.now())
                        .createdAt(Instant.now())
                        .build();
                    paymentRepository.save(payment);
                    log.info("✅ Created payment record for invoice: {}", invoiceId);
                } else {
                    log.info("ℹ️ Payment record already exists for intent: {}", paymentIntentId);
                }

                // Publish event for PDF generation and email notification
                eventPublisher.publishEvent(new InvoicePaidEvent(
                    this,
                    invoice.getInvoiceId(),
                    invoice.getUserId(),
                    amount
                ));
                log.debug("📢 Published InvoicePaidEvent after sync for invoice: {}", invoiceId);

            } else if ("processing".equals(intent.getStatus())) {
                log.info("⏳ Payment still processing: {}", invoiceId);
                // Status remains PENDING
                invoice.setUpdatedAt(Instant.now());
                invoiceRepository.save(invoice);

            } else if ("requires_payment_method".equals(intent.getStatus()) ||
                       "requires_action".equals(intent.getStatus())) {
                log.warn("⚠️ Payment requires action: {} - Status: {}", invoiceId, intent.getStatus());
                // Status remains PENDING but might need user action

            } else {
                log.error("❌ Payment intent in unknown status: {} - {}", paymentIntentId, intent.getStatus());
            }

        } catch (StripeException e) {
            log.error("❌ Error syncing payment status from Stripe: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error syncing payment status: {}", e.getMessage(), e);
        }
    }

    /**
     * Create Stripe customer for user
     */
    private String createStripeCustomer(String userId, String email) throws StripeException {
        log.info("👤 Creating Stripe customer for user: {}", userId);

        CustomerCreateParams params = CustomerCreateParams.builder()
            .setEmail(email != null ? email : "user+" + userId + "@sangrah.local")
            .setName("Sangrah User")  // Default name for India compliance
            .setAddress(
                CustomerCreateParams.Address.builder()
                    .setCountry("IN")  // India
                    .setPostalCode("000000")  // Placeholder
                    .build()
            )
            .setMetadata(java.util.Map.of("userId", userId))
            .build();

        Customer customer = Customer.create(params);
        log.info("✅ Stripe customer created: {}", customer.getId());
        return customer.getId();
    }

    /**
     * Create default billing settings for user
     */
    private UserBillingSettingsDocument createDefaultUserSettings(String userId) {
        log.info("📝 Creating default billing settings for user: {}", userId);

        return UserBillingSettingsDocument.builder()
            .userId(userId)
            .stripeCustomerId(null)
            .totalPaidAllTime(0.0)
            .outstandingBalance(0.0)
            .billing(UserBillingSettingsDocument.BillingAddress.builder()
                .email(null)
                .address(null)
                .city(null)
                .postalCode(null)
                .country(null)
                .taxId(null)
                .build())
            .preferences(UserBillingSettingsDocument.BillingPreferences.builder()
                .autoPayEnabled(true)
                .invoiceEmail(true)
                .costAlertThreshold(100.0)
                .build())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
