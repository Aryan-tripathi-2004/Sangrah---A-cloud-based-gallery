package com.example.Billing.application.service.impl;

import com.example.Billing.api.dto.response.InvoiceDTO;
import com.example.Billing.api.dto.response.PaymentDTO;
import com.example.Billing.api.dto.response.UserBillingSettingsDTO;
import com.example.Billing.application.service.interfaces.ICostEstimationService;
import com.example.Billing.application.service.interfaces.IInvoiceLifecycleService;
import com.example.Billing.application.service.interfaces.IPDFGenerationService;
import com.example.Billing.application.service.interfaces.IStripeIntegrationService;
import com.example.Billing.infrastructure.client.AuthServiceClient;
import com.example.Billing.infrastructure.client.EmailServiceClient;
import com.example.Billing.infrastructure.client.dto.AuthUserResponse;
import com.example.Billing.infrastructure.mapper.InvoiceMapper;
import com.example.Billing.infrastructure.mapper.PaymentMapper;
import com.example.Billing.infrastructure.persistence.document.InvoiceDocument;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final IPDFGenerationService pdfGenerationService;
    private final ICostEstimationService costEstimationService;
    private final IStripeIntegrationService stripeIntegrationService;
    private final AuthServiceClient authServiceClient;
    private final EmailServiceClient emailServiceClient;
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
        log.info("📄 Fetching PDF for invoice {} for user {}", invoiceId, userId);

        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if (!invoice.getUserId().equals(userId)) {
            log.warn("Unauthorized access to invoice PDF {} by user {}", invoiceId, userId);
            throw new ForbiddenOperationException("Unauthorized access to invoice");
        }

        if (invoice.getPdfContent() != null && invoice.getPdfContent().length > 0) {
            log.info("✅ Returning cached PDF for invoice: {} ({} bytes)", invoiceId, invoice.getPdfContent().length);
            return invoice.getPdfContent();
        }

        log.info("⚙️ Generating PDF on-demand for invoice: {}", invoiceId);
        byte[] pdfContent = pdfGenerationService.generateInvoicePDF(invoiceId);

        try {
            invoice.setPdfContent(pdfContent);
            invoice.setPdfGeneratedAt(Instant.now());
            invoiceRepository.save(invoice);
            log.info("💾 Cached generated PDF in database: {} bytes", pdfContent.length);
        } catch (Exception e) {
            log.warn("⚠️ Failed to cache PDF in database (non-critical): {}", e.getMessage());
        }

        return pdfContent;
    }

    @Override
    public Page<InvoiceDTO> getInvoiceHistory(String userId, Pageable pageable) {
        log.info("📄 Fetching paginated invoices for user {} - page: {} size: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        return invoiceRepository.findByUserIdOrderByIssuedDateDesc(userId, pageable)
            .map(invoiceMapper::toDTO);
    }

    @Override
    public List<PaymentDTO> getPaymentHistory(String userId, int page, int size) {
        log.info("💳 Fetching payment history for user {} - page: {} size: {}", userId, page, size);

        return paymentRepository.findByUserIdOrderByTransactionDateDesc(userId, PageRequest.of(page, size))
            .stream()
            .map(paymentMapper::toDTO)
            .toList();
    }

    @Override
    @Transactional
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
                .taxRate(BigDecimal.ZERO)
                .tax(BigDecimal.ZERO)
                .totalAmount(costs.totalCost())
                .build())
            .status(InvoiceStatus.PENDING)
            .issuedDate(Instant.now())
            .dueDate(Instant.now().plusSeconds(invoiceDueDays * 24 * 3600))
            .paidDate(null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Save invoice to database first
        InvoiceDocument savedInvoice = invoiceRepository.save(invoice);
        log.info("✅ Invoice created: {} (total: ${}) (email: {})", invoiceId, costs.totalCost(), userEmail);

        // Generate PDF immediately when invoice is created
        try {
            byte[] pdfContent = pdfGenerationService.generateInvoicePDF(invoiceId);
            savedInvoice.setPdfContent(pdfContent);
            savedInvoice.setPdfGeneratedAt(Instant.now());
            savedInvoice = invoiceRepository.save(savedInvoice);
            log.info("✅ PDF generated immediately on invoice creation: {} bytes", pdfContent.length);
        } catch (Exception e) {
            log.error("❌ PDF generation failed during invoice creation: {}", e.getMessage());
        }

        return invoiceMapper.toDTO(savedInvoice);
    }

    @Override
    @Transactional
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

    @Override
    @Transactional
    public void processPaidInvoiceReceiptAndNotification(String userId, String invoiceId) {
        log.info("💳 [Lifecycle] Processing paid invoice receipt and notification for invoice: {} | user: {}", invoiceId, userId);
        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if (!invoice.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("Unauthorized access to invoice: " + invoiceId);
        }

        String targetEmail = invoice.getUserEmail();
        if (targetEmail == null || targetEmail.isBlank()) {
            targetEmail = getUserEmailFromAuthService(userId);
            invoice.setUserEmail(targetEmail);
        }

        if (invoice.getStatus() != InvoiceStatus.PAID) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidDate(Instant.now());
            invoice.setUpdatedAt(Instant.now());
        }

        try {
            byte[] paidPdfContent = pdfGenerationService.generateInvoicePDF(invoiceId);
            invoice.setPdfContent(paidPdfContent);
            invoice.setPdfGeneratedAt(Instant.now());
        } catch (Exception pdfException) {
            log.warn("⚠️ Failed to regenerate PDF with PAID status: {}", pdfException.getMessage());
        }

        invoiceRepository.save(invoice);

        byte[] emailAttachment = invoice.getPdfContent() != null ? invoice.getPdfContent() : new byte[0];
        BigDecimal amount = (invoice.getCharges() != null && invoice.getCharges().getTotalAmount() != null)
                ? invoice.getCharges().getTotalAmount()
                : BigDecimal.ZERO;

        try {
            emailServiceClient.sendInvoicePaidEmail(
                    EmailServiceClient.EmailSendRequest.builder()
                            .invoiceId(invoiceId)
                            .userId(userId)
                            .userEmail(targetEmail)
                            .amount(amount)
                            .pdfContent(emailAttachment)
                            .emailType("INVOICE_PAID")
                            .build()
            );
            log.info("✅ Payment confirmation email dispatched for invoice: {}", invoiceId);
        } catch (Exception emailEx) {
            log.error("❌ Failed to dispatch payment confirmation email: {}", emailEx.getMessage(), emailEx);
        }
    }

    private String getUserEmailFromAuthService(String userId) {
        try {
            log.debug("📧 Fetching user email from Auth Service for user: {}", userId);
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
        return billingFallbackEmail;
    }
}
