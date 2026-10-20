package com.example.Billing.application.service.impl;

import com.example.Billing.api.dto.response.InvoiceDTO;
import com.example.Billing.api.dto.response.PaymentDTO;
import com.example.Billing.api.dto.response.UserBillingSettingsDTO;
import com.example.Billing.application.service.PDFGenerationService;
import com.example.Billing.application.service.interfaces.ICostEstimationService;
import com.example.Billing.application.service.interfaces.IInvoiceLifecycleService;
import com.example.Billing.application.service.interfaces.IStripeIntegrationService;
import com.example.Billing.infrastructure.client.AuthServiceClient;
import com.example.Billing.infrastructure.client.dto.AuthUserResponse;
import com.example.Billing.infrastructure.mapper.InvoiceMapper;
import com.example.Billing.infrastructure.mapper.PaymentMapper;
import com.example.Billing.infrastructure.persistence.document.InvoiceDocument;
import com.example.Billing.infrastructure.persistence.document.PaymentDocument;
import com.example.Billing.infrastructure.persistence.repository.InvoiceRepository;
import com.example.Billing.infrastructure.persistence.repository.PaymentRepository;
import com.example.Billing.shared.enums.InvoiceStatus;
import com.example.Billing.shared.exception.BillingProcessingException;
import com.example.Billing.shared.exception.ForbiddenOperationException;
import com.example.Billing.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Implementation of {@link IInvoiceLifecycleService}.
 * Handles invoice retrieval, on-demand PDF caching, state transitions, and creation workflows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceLifecycleServiceImpl implements IInvoiceLifecycleService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PDFGenerationService pdfGenerationService;
    private final ICostEstimationService costEstimationService;
    private final IStripeIntegrationService stripeIntegrationService;
    private final AuthServiceClient authServiceClient;
    private final InvoiceMapper invoiceMapper;
    private final PaymentMapper paymentMapper;

    @Value("${billing.fallback.email}")
    private String billingFallbackEmail;

    @Value("${billing.invoice.due-days}")
    private long invoiceDueDays;

    @Override
    public InvoiceDTO getInvoice(String userId, String invoiceId) {
        log.info("📄 Fetching invoice {} for user {}", invoiceId, userId);

        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if (!invoice.getUserId().equals(userId)) {
            log.warn("Unauthorized access to invoice {} by user {}", invoiceId, userId);
            throw new ForbiddenOperationException("Unauthorized access to invoice");
        }

        return invoiceMapper.toDTO(invoice);
    }

    @Override
    public byte[] getInvoicePDF(String userId, String invoiceId) {
        log.info("📥 [PDF Download] Fetching PDF for invoice {} for user {}", invoiceId, userId);

        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> {
                    log.error("❌ [PDF Download] Invoice not found: {}", invoiceId);
                    return new ResourceNotFoundException("Invoice not found: " + invoiceId);
                });

        if (!invoice.getUserId().equals(userId)) {
            log.error("❌ [PDF Download] Unauthorized access to invoice {} by user {}", invoiceId, userId);
            throw new ForbiddenOperationException("Unauthorized access to invoice");
        }

        log.info("📊 [PDF Download] Invoice Status: {} | PDF Generated: {} | PDF Size: {} bytes",
                invoice.getStatus(),
                invoice.getPdfGeneratedAt() != null ? "YES" : "NO",
                invoice.getPdfContent() != null ? invoice.getPdfContent().length : 0);

        // ✨ BEST PRACTICE: Allow download for PENDING invoices too
        // PDF shows status clearly (PENDING or PAID)
        if (invoice.getPdfContent() == null || invoice.getPdfContent().length == 0) {
            log.warn("⚠️ [PDF Download] PDF not available for invoice: {} - Status: {}. Generating on demand...", invoiceId, invoice.getStatus());
            try {
                byte[] generatedPdf = pdfGenerationService.generateInvoicePDF(invoiceId);
                invoice.setPdfContent(generatedPdf);
                invoice.setPdfGeneratedAt(Instant.now());
                invoiceRepository.save(invoice);
                log.info("✅ [PDF Download] PDF generated on demand successful for invoice: {}", invoiceId);
            } catch (Exception e) {
                log.error("❌ [PDF Download] Failed to generate PDF on demand: {}", e.getMessage());
                throw new BillingProcessingException("Invoice PDF could not be generated at this time.");
            }
        }

        if (invoice.getPdfContent() == null || invoice.getPdfContent().length == 0) {
            throw new ResourceNotFoundException("PDF could not be generated for invoice: " + invoiceId);
        }

        log.info("✅ [PDF Download] PDF retrieved successfully for invoice: {} ({} bytes) - Status: {}",
                invoiceId, invoice.getPdfContent().length, invoice.getStatus());
        return invoice.getPdfContent();
    }

    @Override
    public Page<InvoiceDTO> getInvoiceHistory(String userId, Pageable pageable) {
        log.info("📋 Fetching invoice history for user {}", userId);

        return invoiceRepository.findByUserIdOrderByIssuedDateDesc(userId, pageable)
            .map(invoiceMapper::toDTO);
    }

    @Override
    public List<PaymentDTO> getPaymentHistory(String userId, int page, int size) {
        log.info("💳 Fetching payment history for user {} - page: {} size: {}", userId, page, size);

        List<PaymentDocument> payments = paymentRepository.findByUserIdOrderByTransactionDateDesc(userId)
            .stream()
            .skip((long) page * size)
            .limit(size)
            .toList();

        return payments.stream()
            .map(paymentMapper::toDTO)
            .toList();
    }

    @Override
    public InvoiceDTO createInvoice(String userId, String invoiceId, Instant startDate, Instant endDate) {
        log.info("💰 Creating invoice {} for user {} from {} to {}", invoiceId, userId, startDate, endDate);

        // Check if invoice already exists
        invoiceRepository.findByUserIdAndBillingPeriod_StartDateAndBillingPeriod_EndDate(
            userId, startDate, endDate
        ).ifPresent(existing -> {
            throw new BillingProcessingException("Invoice already exists for this period");
        });

        // Calculate charges from StorageUsageLedger
        ICostEstimationService.BillingCostDetails costs =
            costEstimationService.calculateCostForPeriod(userId, startDate, endDate);

        // Get user settings (for tax rate, etc.) via StripeIntegrationService
        UserBillingSettingsDTO settings = stripeIntegrationService.getUserSettings(userId);

        // Fetch user email from Auth Service
        String userEmail = getUserEmailFromAuthService(userId);
        log.info("📧 [Invoice Creation] User email fetched: {}", userEmail);

        // Build invoice
        InvoiceDocument invoice = InvoiceDocument.builder()
            .invoiceId(invoiceId)
            .userId(userId)
            .userEmail(userEmail)
            .billingPeriod(InvoiceDocument.BillingPeriod.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build())
            .storageMetrics(InvoiceDocument.StorageMetrics.builder()
                .imageGBDays(costs.imageGBDays())
                .imageCost(costs.imageCost())
                .videoGBDays(costs.videoGBDays())
                .videoCost(costs.videoCost())
                .totalGBDays(costs.totalGBDays())
                .build())
            .charges(InvoiceDocument.Charges.builder()
                .storageRate(BigDecimal.valueOf(0.01))
                .subtotal(costs.totalCost())
                .taxRate(BigDecimal.ZERO)  // TODO: Get tax rate from settings
                .tax(BigDecimal.ZERO)
                .totalAmount(costs.totalCost())
                .build())
            .status(InvoiceStatus.PENDING)
            .issuedDate(Instant.now())
            .dueDate(Instant.now().plusSeconds(invoiceDueDays * 24 * 3600))  // configurable days from now
            .paidDate(null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Save invoice to database first
        InvoiceDocument savedInvoice = invoiceRepository.save(invoice);
        log.info("✅ Invoice created: {} (total: ${}) (email: {})", invoiceId, costs.totalCost(), userEmail);

        // ✨ BEST PRACTICE: Generate PDF immediately when invoice is created (not on payment)
        try {
            byte[] pdfContent = pdfGenerationService.generateInvoicePDF(invoiceId);
            savedInvoice.setPdfContent(pdfContent);
            savedInvoice.setPdfGeneratedAt(Instant.now());
            invoiceRepository.save(savedInvoice);
            log.info("✅ [BEST PRACTICE] PDF generated immediately on invoice creation: {} bytes", pdfContent.length);
        } catch (Exception e) {
            log.error("❌ PDF generation failed during invoice creation: {}", e.getMessage());
        }

        return invoiceMapper.toDTO(savedInvoice);
    }

    @Override
    public InvoiceDTO markInvoiceAsPaid(String userId, String invoiceId) {
        log.info("💳 Marking invoice {} as PAID for user {}", invoiceId, userId);

        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getUserId().equals(userId)) {
            log.warn("Unauthorized attempt to mark invoice {} as paid by user {}", invoiceId, userId);
            throw new ForbiddenOperationException("Unauthorized access to invoice");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidDate(Instant.now());
        invoice.setUpdatedAt(Instant.now());

        InvoiceDocument saved = invoiceRepository.save(invoice);
        log.info("✅ Invoice marked as PAID: {}", invoiceId);

        return invoiceMapper.toDTO(saved);
    }

    /**
     * Fetch user email from Auth Service
     * Called when creating invoice to ensure email is available for payment notifications
     */
    private String getUserEmailFromAuthService(String userId) {
        try {
            log.debug("📧 Fetching user email from Auth Service for user: {}", userId);
            AuthUserResponse response = authServiceClient.getUserById(userId);

            if (response != null && response.data() != null) {
                String email = response.data().email();

                if (email != null && !email.isEmpty()) {
                    log.info("✅ User email fetched from Auth Service: {}", email);
                    return email;
                }
            }

            log.warn("⚠️ Could not fetch user email from Auth Service, using fallback");
            return billingFallbackEmail;

        } catch (Exception e) {
            log.warn("⚠️ Failed to fetch user email from Auth Service: {}", e.getMessage());
            return billingFallbackEmail;
        }
    }
}
