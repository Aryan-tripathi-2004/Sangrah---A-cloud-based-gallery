package com.example.Billing.infrastructure.event;

import com.example.Billing.application.service.PDFGenerationService;
import com.example.Billing.infrastructure.client.AuthServiceClient;
import com.example.Billing.infrastructure.client.EmailServiceClient;
import com.example.Billing.infrastructure.client.dto.AuthUserResponse;
import com.example.Billing.infrastructure.persistence.document.InvoiceDocument;
import com.example.Billing.infrastructure.persistence.repository.InvoiceRepository;
import com.example.Billing.shared.enums.InvoiceStatus;
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
    private final AuthServiceClient authServiceClient;

    /**
     * Fetch user email from Auth Service
     * Fallback for old invoices without userEmail
     */
    private String getUserEmailFromAuthService(String userId) {
        try {
            log.debug("📧 [Event Hook] Fetching user email from Auth Service for user: {}", userId);
            AuthUserResponse response = authServiceClient.getUserById(userId);
            if (response != null && response.data() != null) {
                String email = response.data().email();
                if (email != null && !email.isEmpty()) {
                    return email;
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to fetch user email: {}", e.getMessage());
        }
        return "billing@sangrah.com"; // Fallback
    }

    @EventListener
    public void onInvoicePaid(InvoicePaidEvent event) {
        log.info("💳 ╔═══════════════════════════════════════════════════════════════╗");
        log.info("💳 ║ [PAYMENT EVENT] Processing InvoicePaidEvent                  ║");
        log.info("💳 ║ Invoice: {} | User: {} | Amount: ${}                        ║",
                event.getInvoiceId(), event.getUserId(), event.getTotalAmount());
        log.info("💳 ╚═══════════════════════════════════════════════════════════════╝");

        try {
            // ✨ BEST PRACTICE CHANGE: PDF already generated at invoice creation
            // No need to regenerate - just use existing PDF for email

            log.info("📋 [Step 1/3] Fetching invoice {} for payment...", event.getInvoiceId());
            InvoiceDocument invoice = invoiceRepository.findByInvoiceId(event.getInvoiceId())
                    .orElseThrow(() -> {
                        log.error("❌ [Step 1/3] Invoice not found: {}", event.getInvoiceId());
                        return new RuntimeException("Invoice not found: " + event.getInvoiceId());
                    });

            log.info("📋 [Step 1/3] Invoice found - Email: {} | Status: {} | Amount: ${}",
                    invoice.getUserEmail(), invoice.getStatus(), invoice.getCharges().getTotalAmount());

            // ✨ NEW: Ensure userEmail is populated (fallback for old test invoices)
            String targetEmail = invoice.getUserEmail();
            if (targetEmail == null || targetEmail.isEmpty()) {
                log.warn("⚠️ Invoice {} missing userEmail, fetching from Auth Service...", invoice.getInvoiceId());
                targetEmail = getUserEmailFromAuthService(event.getUserId());
                invoice.setUserEmail(targetEmail);
                // Save it so it's not missing next time
                invoiceRepository.save(invoice);
            }

            // Verify PDF exists (should have been generated at invoice creation)
            if (invoice.getPdfContent() == null || invoice.getPdfContent().length == 0) {
                log.warn("⚠️ [Step 1/3] PDF is missing - regenerating...");
                // Fallback: regenerate if somehow missing
                byte[] pdfContent = pdfGenerationService.generateInvoicePDF(event.getInvoiceId());
                invoice.setPdfContent(pdfContent);
                invoice.setPdfGeneratedAt(Instant.now());
                invoiceRepository.save(invoice);
            }

            log.info("✅ [Step 1/3] PDF ready: {} bytes", invoice.getPdfContent().length);

            // Step 2: Update invoice status to PAID
            log.info("💳 [Step 2/3] Marking invoice as PAID...");
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidDate(Instant.now());
            invoiceRepository.save(invoice);
            log.info("✅ [Step 2/3] Invoice marked as PAID");

            // ✨ NEW: Regenerate PDF after status change so it shows PAID instead of PENDING
            log.info("📄 [Step 2.5/3] Regenerating PDF with PAID status...");
            try {
                byte[] updatedPdfContent = pdfGenerationService.generateInvoicePDF(event.getInvoiceId());
                invoice.setPdfContent(updatedPdfContent);
                invoice.setPdfGeneratedAt(Instant.now());
                invoiceRepository.save(invoice);
                log.info("✅ [Step 2.5/3] PDF regenerated successfully with PAID status: {} bytes", updatedPdfContent.length);
            } catch (Exception pdfException) {
                log.warn("⚠️ [Step 2.5/3] Failed to regenerate PDF with PAID status: {}", pdfException.getMessage());
                // Continue with old PDF content - not critical
            }

            // Step 3: Send email with PDF showing PAID status
            log.info("📧 [Step 3/3] Sending payment confirmation email...");
            log.info("📧 [Step 3/3] To: {} | Invoice: {} | Amount: ${} | PDF: {} bytes",
                    targetEmail, event.getInvoiceId(), invoice.getCharges().getTotalAmount(),
                    invoice.getPdfContent().length);

            try {
                emailServiceClient.sendInvoicePaidEmail(
                        EmailServiceClient.EmailSendRequest.builder()
                                .invoiceId(event.getInvoiceId())
                                .userId(event.getUserId())
                                .amount(invoice.getCharges().getTotalAmount())
                                .pdfContent(invoice.getPdfContent())
                                .userEmail(targetEmail)
                                .emailType("invoice-paid")
                                .build()
                );
                log.info("✅ [Step 3/3] Email sent successfully!");
            } catch (Exception emailException) {
                log.warn("⚠️ [Step 3/3] Email service call failed (non-critical): {}", emailException.getMessage());
                // Don't rethrow - invoice is paid and PDF exists, email can be retried
            }

            log.info("✅ ╔═══════════════════════════════════════════════════════════════╗");
            log.info("✅ ║ [PAYMENT EVENT] COMPLETED SUCCESSFULLY                       ║");
            log.info("✅ ║ Invoice: {} | Status: PAID | Email: {}",
                    event.getInvoiceId(), targetEmail);
            log.info("✅ ╚═══════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            log.error("❌ ╔═══════════════════════════════════════════════════════════════╗");
            log.error("❌ ║ [PAYMENT EVENT] ERROR processing payment event              ║");
            log.error("❌ ║ Invoice: {} | Error: {}", event.getInvoiceId(), e.getMessage());
            log.error("❌ ╚═══════════════════════════════════════════════════════════════╝", e);
            // Don't throw - payment already confirmed, this is post-processing
        }
    }
}
