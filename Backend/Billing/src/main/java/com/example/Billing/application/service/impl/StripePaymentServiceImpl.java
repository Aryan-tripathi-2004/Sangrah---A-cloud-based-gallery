package com.example.Billing.application.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.example.Billing.api.dto.response.CheckoutSessionResponse;
import com.example.Billing.api.dto.response.PaymentStatusResponse;
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
import com.example.Billing.shared.exception.BillingProcessingException;
import com.example.Billing.shared.exception.ForbiddenOperationException;
import com.example.Billing.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${billing.currency}")
    private String billingCurrency;

    @Value("${billing.customer.default.country}")
    private String defaultCustomerCountry;

    @Value("${billing.customer.default.postal-code}")
    private String defaultCustomerPostalCode;

    @Value("${billing.payment.retry.max-attempts}")
    private int maxPaymentRetries;

    @Value("${billing.payment.retry.interval-hours}")
    private long paymentRetryIntervalHours;

    @Value("${billing.default.cost-alert-threshold}")
    private double defaultCostAlertThreshold;

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
    public String createPaymentIntent(String userId, String invoiceId, BigDecimal amount)
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
            .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())  // Convert to cents
            .setCurrency(billingCurrency)
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
    @Override
    @Transactional
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
            BigDecimal amount = (intent.getAmount() != null) ? BigDecimal.valueOf(intent.getAmount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) :
                           (invoice.getCharges() != null ? invoice.getCharges().getTotalAmount() : BigDecimal.ZERO);

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
            throw new BillingProcessingException("Stripe communication failed during payment success sync.", e);
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
            BigDecimal amount = (invoice.getCharges() != null) ? invoice.getCharges().getTotalAmount() :
                           (intent.getAmount() != null ? BigDecimal.valueOf(intent.getAmount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

            PaymentDocument payment = PaymentDocument.builder()
                .userId(invoice.getUserId())
                .invoiceId(invoice.getId())
                .amount(amount)
                .status(PaymentStatus.FAILED)
                .stripePaymentIntentId(paymentIntentId)
                .failureReason(failureReason)
                .retryCount(0)
                .nextRetryDate(Instant.now().plus(Duration.ofHours(paymentRetryIntervalHours)))
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
            throw new BillingProcessingException("Stripe communication failed during payment failure sync.", e);
        }
    }

    /**
     * Auto-retry failed payments
     * Runs every hour to check for payments due for retry
     */
    @Scheduled(fixedRateString = "${billing.payment.retry.check-interval-ms:3600000}")  // Default: every hour
    public void retryFailedPayments() {
        log.info("🔄 Checking for failed payments to retry...");

        try {
            List<PaymentDocument> failedPayments = paymentRepository
                .findByStatusAndNextRetryDateLessThanEqualOrderByNextRetryDateAsc(PaymentStatus.FAILED, Instant.now());

            log.info("📊 Found {} payments due for retry", failedPayments.size());

            for (PaymentDocument payment : failedPayments) {
                if (payment.getRetryCount() >= maxPaymentRetries) {
                    log.warn("⚠️ Payment {} exceeded max retries ({})", payment.getId(), maxPaymentRetries);
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
                            payment.setNextRetryDate(Instant.now().plus(Duration.ofHours(paymentRetryIntervalHours)));
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
    @Override
    @Transactional
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
                BigDecimal amount = (intent.getAmount() != null) ? BigDecimal.valueOf(intent.getAmount()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) :
                               (invoice.getCharges() != null ? invoice.getCharges().getTotalAmount() : BigDecimal.ZERO);

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
            throw new BillingProcessingException("Stripe communication failed during payment status sync.", e);
        } catch (Exception e) {
            log.error("❌ Unexpected error syncing payment status: {}", e.getMessage(), e);
            throw new BillingProcessingException("Failed to sync payment status due to an internal error.", e);
        }
    }

    @Override
    @Transactional
    public PaymentStatusResponse syncPaymentStatus(String invoiceId, String userId) {
        log.info("🔄 Syncing payment status for invoice: {} user: {}", invoiceId, userId);
        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if (!invoice.getUserId().equals(userId)) {
            log.warn("⚠️ Unauthorized sync attempt by user {} for invoice {}", userId, invoiceId);
            throw new ForbiddenOperationException("Unauthorized access to invoice");
        }

        if (invoice.getPaymentIntentId() == null || invoice.getPaymentIntentId().isEmpty()) {
            log.warn("⚠️ No payment intent ID for invoice: {}", invoiceId);
            return getPaymentStatus(userId, invoiceId);
        }

        syncPaymentStatusWithStripe(invoiceId, invoice.getPaymentIntentId());
        return getPaymentStatus(userId, invoiceId);
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
                    .setCountry(defaultCustomerCountry)
                    .setPostalCode(defaultCustomerPostalCode)  // Placeholder
                    .build()
            )
            .setMetadata(Map.of("userId", userId))
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
            .totalPaidAllTime(BigDecimal.ZERO)
            .outstandingBalance(BigDecimal.ZERO)
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
                .costAlertThreshold(BigDecimal.valueOf(defaultCostAlertThreshold))
                .build())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    /**
     * Creates a Stripe checkout session for an invoice.
     * Fetches the invoice amount internally and delegates to createPaymentIntent.
     */
    @Override
    public CheckoutSessionResponse createCheckoutSession(String userId, String invoiceId) throws StripeException {
        log.info("💳 [createCheckoutSession] Fetching invoice {} for user {}", invoiceId, userId);
        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        // Strictly enforce ownership perimeter before processing
        if (!invoice.getUserId().equals(userId)) {
            log.warn("⚠️ Unauthorized checkout attempt by user {} for invoice {}", userId, invoiceId);
            throw new ForbiddenOperationException("Unauthorized access to invoice");
        }

        BigDecimal amount = invoice.getCharges() != null ? invoice.getCharges().getTotalAmount() : BigDecimal.ZERO;
        String clientSecret = createPaymentIntent(userId, invoiceId, amount);

        log.info("✅ [createCheckoutSession] Checkout session created for invoice: {}", invoiceId);
        return CheckoutSessionResponse.builder()
                .invoiceId(invoiceId)
                .clientSecret(clientSecret)
                .amount(amount)
                .currency(billingCurrency)
                .build();
    }

    /**
     * Returns the current payment status of an invoice.
     */
    @Override
    public PaymentStatusResponse getPaymentStatus(String userId, String invoiceId) {
        log.info("📊 [getPaymentStatus] Fetching invoice {} for user {}", invoiceId, userId);
        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if (!invoice.getUserId().equals(userId)) {
            log.warn("⚠️ Unauthorized payment status attempt by user {} for invoice {}", userId, invoiceId);
            throw new ForbiddenOperationException("Unauthorized access to invoice");
        }

        return PaymentStatusResponse.builder()
                .invoiceId(invoiceId)
                .status(invoice.getStatus() != null ? invoice.getStatus().name() : "UNKNOWN")
                .paidDate(invoice.getPaidDate())
                .build();
    }
}



