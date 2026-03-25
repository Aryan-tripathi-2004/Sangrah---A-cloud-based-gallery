package com.example.Billing.infrastructure.event;

import com.example.Billing.application.service.PDFGenerationService;
import com.example.Billing.infrastructure.client.EmailServiceClient;
import com.example.Billing.infrastructure.persistence.document.InvoiceDocument;
import com.example.Billing.infrastructure.persistence.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoicePaidEventListener {

    private final PDFGenerationService pdfGenerationService;
    private final InvoiceRepository invoiceRepository;
    private final EmailServiceClient emailServiceClient;

    @EventListener
    public void onInvoicePaid(InvoicePaidEvent event) {
        log.info("💳 Processing InvoicePaidEvent for invoice: {}", event.getInvoiceId());

        try {
            // Step 1: Generate PDF (only after payment succeeds)
            log.info("📄 Generating PDF for invoice: {}", event.getInvoiceId());
            byte[] pdfContent = pdfGenerationService.generateInvoicePDF(event.getInvoiceId());

            // Step 2: Save PDF to invoice document
            log.info("💾 Saving PDF to invoice document: {}", event.getInvoiceId());
            InvoiceDocument invoice = invoiceRepository.findByInvoiceId(event.getInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found: " + event.getInvoiceId()));

            invoice.setPdfContent(pdfContent);
            invoice.setPdfGeneratedAt(Instant.now());
            invoiceRepository.save(invoice);

            log.info("✅ PDF saved successfully: {} bytes for invoice: {}", pdfContent.length, event.getInvoiceId());

            // Step 3: Call Email Service to send email with PDF
            try {
                log.info("📧 Calling Email Service to send payment confirmation email");
                emailServiceClient.sendInvoicePaidEmail(
                        event.getInvoiceId(),
                        event.getUserId(),
                        invoice.getCharges().getTotalAmount(),
                        pdfContent,
                        invoice.getUserEmail()
                );
                log.info("✉️ Email service called successfully for invoice: {}", event.getInvoiceId());
            } catch (Exception emailException) {
                log.warn("⚠️ Email service call failed, but PDF was saved. Invoice: {} Error: {}",
                        event.getInvoiceId(), emailException.getMessage());
                // Don't rethrow - invoice is paid and PDF exists, email can be retried
            }

        } catch (Exception e) {
            log.error("❌ Error processing InvoicePaidEvent for invoice {}: {}",
                    event.getInvoiceId(), e.getMessage(), e);
            // Don't throw - payment already confirmed, this is post-processing
            // PDF generation failure should not block invoice payment confirmation
        }
    }
}
