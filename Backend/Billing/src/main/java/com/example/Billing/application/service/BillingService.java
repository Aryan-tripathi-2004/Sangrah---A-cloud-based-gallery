package com.example.Billing.application.service;

import com.example.Billing.api.dto.response.InvoiceDTO;
import com.example.Billing.api.dto.response.PaymentDTO;
import com.example.Billing.infrastructure.persistence.document.InvoiceDocument;
import com.example.Billing.infrastructure.persistence.document.PaymentDocument;
import com.example.Billing.infrastructure.persistence.document.UserBillingSettingsDocument;
import com.example.Billing.infrastructure.persistence.repository.InvoiceRepository;
import com.example.Billing.infrastructure.persistence.repository.PaymentRepository;
import com.example.Billing.infrastructure.persistence.repository.UserBillingSettingsRepository;
import com.example.Billing.shared.util.BillingCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final UserBillingSettingsRepository userSettingsRepository;
    private final CostEstimationService costEstimationService;
    private final BillingCalculator calculator;

    /**
     * Get invoice details
     */
    public InvoiceDTO getInvoice(String userId, String invoiceId) {
        log.info("📄 Fetching invoice {} for user {}", invoiceId, userId);

        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        if (!invoice.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to invoice");
        }

        return mapToDTO(invoice);
    }

    /**
     * Get invoice PDF content
     */
    public byte[] getInvoicePDF(String userId, String invoiceId) {
        log.info("📥 Fetching PDF for invoice {} for user {}", invoiceId, userId);

        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        if (!invoice.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to invoice");
        }

        if (invoice.getPdfContent() == null) {
            log.warn("⚠️ PDF not yet generated for invoice: {}", invoiceId);
            throw new RuntimeException("PDF not yet available for this invoice");
        }

        log.info("✅ PDF retrieved for invoice: {}", invoiceId);
        return invoice.getPdfContent();
    }

    /**
     * Get invoice history for user (paginated)
     */
    public Page<InvoiceDTO> getInvoiceHistory(String userId, Pageable pageable) {
        log.info("📋 Fetching invoice history for user {}", userId);

        return invoiceRepository.findByUserIdOrderByIssuedDateDesc(userId, pageable)
            .map(this::mapToDTO);
    }

    /**
     * Get payment history for user
     */
    public List<PaymentDTO> getPaymentHistory(String userId, int page, int size) {
        log.info("💳 Fetching payment history for user {} - page: {} size: {}", userId, page, size);

        // For now, return empty list since we don't have proper pagination in the payment repository
        // In production, implement proper pagination in PaymentRepository
        List<PaymentDocument> payments = paymentRepository.findByUserIdOrderByTransactionDateDesc(userId)
            .stream()
            .skip((long) page * size)
            .limit(size)
            .toList();

        return payments.stream()
            .map(this::mapPaymentToDTO)
            .toList();
    }

    /**
     * Create invoice for a user and billing period
     * Called by InvoiceGenerationService
     */
    public InvoiceDTO createInvoice(String userId, String invoiceId, Instant startDate, Instant endDate) {
        log.info("💰 Creating invoice {} for user {} from {} to {}", invoiceId, userId, startDate, endDate);

        // Check if invoice already exists
        invoiceRepository.findByUserIdAndBillingPeriod_StartDateAndBillingPeriod_EndDate(
            userId, startDate, endDate
        ).ifPresent(existing -> {
            throw new RuntimeException("Invoice already exists for this period");
        });

        // Calculate charges from StorageUsageLedger
        CostEstimationService.BillingCostDetails costs =
            costEstimationService.calculateCostForPeriod(userId, startDate, endDate);

        // Get user settings (for tax rate, etc.)
        UserBillingSettingsDocument settings = userSettingsRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultUserSettings(userId));

        // Build invoice
        InvoiceDocument invoice = InvoiceDocument.builder()
            .invoiceId(invoiceId)
            .userId(userId)
            .billingPeriod(InvoiceDocument.BillingPeriod.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build())
            .storageMetrics(InvoiceDocument.StorageMetrics.builder()
                .imageGBDays(costs.getImageGBDays())
                .imageCost(costs.getImageCost())
                .videoGBDays(costs.getVideoGBDays())
                .videoCost(costs.getVideoCost())
                .totalGBDays(costs.getTotalGBDays())
                .build())
            .charges(InvoiceDocument.Charges.builder()
                .storageRate(BillingCalculator.STORAGE_RATE_PER_GB_DAY)
                .subtotal(costs.getTotalCost())
                .taxRate(0.0)  // TODO: Get tax rate from settings
                .tax(0.0)
                .totalAmount(costs.getTotalCost())
                .build())
            .status("PENDING")
            .issuedDate(Instant.now())
            .dueDate(Instant.now().plusSeconds(15 * 24 * 3600))  // 15 days from now
            .paidDate(null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        InvoiceDocument savedInvoice = invoiceRepository.save(invoice);
        log.info("✅ Invoice created: {} (total: ${})", invoiceId, costs.getTotalCost());

        return mapToDTO(savedInvoice);
    }

    /**
     * Mark invoice as paid
     */
    public InvoiceDTO markInvoiceAsPaid(String userId, String invoiceId) {
        log.info("💳 Marking invoice {} as PAID for user {}", invoiceId, userId);

        InvoiceDocument invoice = invoiceRepository.findByInvoiceId(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (!invoice.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to invoice");
        }

        invoice.setStatus("PAID");
        invoice.setPaidDate(Instant.now());
        invoice.setUpdatedAt(Instant.now());

        InvoiceDocument saved = invoiceRepository.save(invoice);
        log.info("✅ Invoice marked as PAID: {}", invoiceId);

        return mapToDTO(saved);
    }

    /**
     * Get user billing settings (or create defaults)
     */
    public UserBillingSettingsDocument getUserSettings(String userId) {
        return userSettingsRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultUserSettings(userId));
    }

    /**
     * Create default billing settings for new user
     */
    private UserBillingSettingsDocument createDefaultUserSettings(String userId) {
        log.info("📝 Creating default billing settings for user {}", userId);

        UserBillingSettingsDocument settings = UserBillingSettingsDocument.builder()
            .userId(userId)
            .stripeCustomerId(null)  // Will be set when user adds payment method
            .paymentMethods(null)
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

        return userSettingsRepository.save(settings);
    }

    /**
     * Convert document to DTO
     */
    private InvoiceDTO mapToDTO(InvoiceDocument doc) {
        return InvoiceDTO.builder()
            .id(doc.getId())
            .invoiceId(doc.getInvoiceId())
            .userId(doc.getUserId())
            .billingPeriod(InvoiceDTO.BillingPeriodDTO.builder()
                .startDate(doc.getBillingPeriod().getStartDate())
                .endDate(doc.getBillingPeriod().getEndDate())
                .build())
            .storageMetrics(InvoiceDTO.StorageMetricsDTO.builder()
                .imageGBDays(doc.getStorageMetrics().getImageGBDays())
                .imageCost(doc.getStorageMetrics().getImageCost())
                .videoGBDays(doc.getStorageMetrics().getVideoGBDays())
                .videoCost(doc.getStorageMetrics().getVideoCost())
                .totalGBDays(doc.getStorageMetrics().getTotalGBDays())
                .build())
            .charges(InvoiceDTO.ChargesDTO.builder()
                .storageRate(doc.getCharges().getStorageRate())
                .subtotal(doc.getCharges().getSubtotal())
                .taxRate(doc.getCharges().getTaxRate())
                .tax(doc.getCharges().getTax())
                .totalAmount(doc.getCharges().getTotalAmount())
                .build())
            .status(doc.getStatus())
            .issuedDate(doc.getIssuedDate())
            .dueDate(doc.getDueDate())
            .paidDate(doc.getPaidDate())
            .build();
    }

    /**
     * Convert PaymentDocument to PaymentDTO
     */
    private PaymentDTO mapPaymentToDTO(PaymentDocument doc) {
        return PaymentDTO.builder()
            .id(doc.getId())
            .invoiceId(doc.getInvoiceId())
            .amount(doc.getAmount())
            .status(doc.getStatus())
            .transactionDate(doc.getTransactionDate())
            .stripeChargeId(doc.getStripeChargeId())
            .failureReason(doc.getFailureReason())
            .build();
    }
}
