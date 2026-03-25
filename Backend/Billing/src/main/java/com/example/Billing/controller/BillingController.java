package com.example.Billing.controller;

import com.example.Billing.dto.InvoiceResponse;
import com.example.Billing.dto.PaymentRequest;
import com.example.Billing.model.Invoice;
import com.example.Billing.repository.InvoiceRepository;
import com.example.Billing.service.BillingService;
import com.example.Billing.service.PDFGenerationService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final PDFGenerationService pdfGenerationService;
    private final InvoiceRepository invoiceRepository;

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @PostMapping("/payment")
    public ResponseEntity<InvoiceResponse> createPayment(@RequestBody PaymentRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(billingService.createPayment(request));
        } catch (StripeException ex) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build();
        }
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceResponse>> getInvoices(@RequestParam String userEmail) {
        return ResponseEntity.ok(billingService.getInvoicesForUser(userEmail));
    }

    @GetMapping("/invoices/{invoiceNumber}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(billingService.getInvoiceByNumber(invoiceNumber));
    }

    /**
     * Downloads a PDF invoice for the given invoice number.
     */
    @GetMapping("/invoices/{invoiceNumber}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceNumber));
        try {
            byte[] pdfBytes = pdfGenerationService.generateInvoicePdf(invoice);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "invoice-" + invoiceNumber + ".pdf");
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Stripe webhook endpoint – receives payment_intent.succeeded events and
     * updates the invoice status to PAID in the database.
     *
     * <p>When {@code STRIPE_WEBHOOK_SECRET} is configured, the Stripe-Signature
     * header is verified before processing the event. This guards against forged
     * webhook requests. Set the {@code stripe.webhook-secret} property to your
     * Stripe webhook signing secret (found in the Stripe Dashboard).</p>
     */
    @PostMapping("/webhook/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        try {
            com.stripe.model.Event event;

            if (stripeWebhookSecret != null && !stripeWebhookSecret.isBlank()
                    && sigHeader != null) {
                // Verify signature when a webhook secret is configured (recommended for production)
                event = com.stripe.net.Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
            } else {
                // Fallback: parse without signature verification (development only)
                event = com.stripe.net.ApiResource.GSON
                        .fromJson(payload, com.stripe.model.Event.class);
            }

            if ("payment_intent.succeeded".equals(event.getType())) {
                com.stripe.model.StripeObject stripeObject = event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                if (stripeObject instanceof com.stripe.model.PaymentIntent pi) {
                    billingService.markInvoicePaid(pi.getId());
                }
            }
        } catch (com.stripe.exception.SignatureVerificationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
