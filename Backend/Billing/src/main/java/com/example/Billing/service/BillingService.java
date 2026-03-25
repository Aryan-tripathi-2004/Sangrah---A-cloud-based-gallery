package com.example.Billing.service;

import com.example.Billing.dto.InvoiceResponse;
import com.example.Billing.dto.PaymentRequest;
import com.example.Billing.model.Invoice;
import com.example.Billing.model.InvoiceStatus;
import com.example.Billing.repository.InvoiceRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final InvoiceRepository invoiceRepository;

    /**
     * Creates a Stripe PaymentIntent and a corresponding Invoice record.
     * The invoice starts in PENDING state and is updated to PAID once the
     * payment succeeds, or to FAILED if an error occurs.
     */
    @Transactional
    public InvoiceResponse createPayment(PaymentRequest request) throws StripeException {
        // Create the invoice first (PENDING state)
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .userEmail(request.getUserEmail())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency().toLowerCase() : "inr")
                .status(InvoiceStatus.PENDING)
                .description(request.getDescription())
                .build();
        invoice = invoiceRepository.save(invoice);

        try {
            // Build Stripe PaymentIntent parameters
            long amountInSmallestUnit = request.getAmount()
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInSmallestUnit)
                    .setCurrency(invoice.getCurrency())
                    .setDescription(request.getDescription())
                    .setPaymentMethod(request.getPaymentMethodId())
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(
                                            PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Update invoice status based on Stripe response
            invoice.setStripePaymentIntentId(paymentIntent.getId());
            if ("succeeded".equals(paymentIntent.getStatus())) {
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setPaidAt(LocalDateTime.now());
            } else {
                invoice.setStatus(InvoiceStatus.PENDING);
            }
            invoice = invoiceRepository.save(invoice);

        } catch (StripeException ex) {
            // Mark the invoice as FAILED so the state is consistent in the DB
            invoice.setStatus(InvoiceStatus.FAILED);
            invoiceRepository.save(invoice);
            throw ex;
        }

        return toResponse(invoice);
    }

    /**
     * Called by the Stripe webhook handler (or a polling mechanism) to mark an
     * invoice as PAID once the payment_intent.succeeded event is received.
     * This ensures the invoice status is always up-to-date even when the
     * initial confirm call does not return a "succeeded" status immediately.
     */
    @Transactional
    public InvoiceResponse markInvoicePaid(String paymentIntentId) {
        Invoice invoice = invoiceRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException(
                        "Invoice not found for PaymentIntent: " + paymentIntentId));

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoice = invoiceRepository.save(invoice);

        return toResponse(invoice);
    }

    public List<InvoiceResponse> getInvoicesForUser(String userEmail) {
        return invoiceRepository.findByUserEmail(userEmail)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceNumber));
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .userEmail(invoice.getUserEmail())
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .stripePaymentIntentId(invoice.getStripePaymentIntentId())
                .description(invoice.getDescription())
                .createdAt(invoice.getCreatedAt())
                .paidAt(invoice.getPaidAt())
                .build();
    }
}
