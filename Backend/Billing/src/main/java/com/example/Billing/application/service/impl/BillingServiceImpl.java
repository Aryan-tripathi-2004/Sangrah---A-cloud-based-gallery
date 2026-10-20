package com.example.Billing.application.service.impl;

import com.example.Billing.api.dto.response.HealthResponse;
import com.example.Billing.api.dto.response.InvoiceDTO;
import com.example.Billing.api.dto.response.PaymentDTO;
import com.example.Billing.api.dto.response.StorageUsageLedgerDTO;
import com.example.Billing.api.dto.response.StorageUsageSummaryResponse;
import com.example.Billing.api.dto.response.StripeConfigResponse;
import com.example.Billing.application.service.interfaces.IBillingService;
import com.example.Billing.application.service.interfaces.IInvoiceLifecycleService;
import com.example.Billing.application.service.interfaces.IStorageMeteringService;
import com.example.Billing.application.service.interfaces.IStripeIntegrationService;
import com.example.Billing.api.dto.response.UserBillingSettingsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Facade implementation of {@link IBillingService}.
 * Coordinates dedicated, single-responsibility services:
 * <ul>
 *   <li>{@link IInvoiceLifecycleService} for invoice management and state transitions.</li>
 *   <li>{@link IStripeIntegrationService} for customer identities and payment gateway configuration.</li>
 *   <li>{@link IStorageMeteringService} for ledger queries and storage consumption analytics.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements IBillingService {

    private final IInvoiceLifecycleService invoiceLifecycleService;
    private final IStripeIntegrationService stripeIntegrationService;
    private final IStorageMeteringService storageMeteringService;

    @Override
    public InvoiceDTO getInvoice(String userId, String invoiceId) {
        return invoiceLifecycleService.getInvoice(userId, invoiceId);
    }

    @Override
    public byte[] getInvoicePDF(String userId, String invoiceId) {
        return invoiceLifecycleService.getInvoicePDF(userId, invoiceId);
    }

    @Override
    public Page<InvoiceDTO> getInvoiceHistory(String userId, Pageable pageable) {
        return invoiceLifecycleService.getInvoiceHistory(userId, pageable);
    }

    @Override
    public List<PaymentDTO> getPaymentHistory(String userId, int page, int size) {
        return invoiceLifecycleService.getPaymentHistory(userId, page, size);
    }

    @Override
    public InvoiceDTO createInvoice(String userId, String invoiceId, Instant startDate, Instant endDate) {
        return invoiceLifecycleService.createInvoice(userId, invoiceId, startDate, endDate);
    }

    @Override
    public InvoiceDTO markInvoiceAsPaid(String userId, String invoiceId) {
        return invoiceLifecycleService.markInvoiceAsPaid(userId, invoiceId);
    }

    @Override
    public UserBillingSettingsDTO getUserSettings(String userId) {
        return stripeIntegrationService.getUserSettings(userId);
    }

    @Override
    public StorageUsageSummaryResponse getStorageUsageSummary(String userId) {
        return storageMeteringService.getStorageUsageSummary(userId);
    }

    @Override
    public List<StorageUsageLedgerDTO> getStorageUsageDetails(String userId) {
        return storageMeteringService.getStorageUsageDetails(userId);
    }

    @Override
    public StripeConfigResponse getStripeConfig() {
        return stripeIntegrationService.getStripeConfig();
    }

    @Override
    public HealthResponse getHealth() {
        return new HealthResponse("Billing service is running");
    }
}
