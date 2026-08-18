package com.example.Billing.application.service.interfaces;

import com.example.Billing.api.dto.response.InvoiceDTO;
import com.example.Billing.api.dto.response.PaymentDTO;
import com.example.Billing.infrastructure.persistence.document.UserBillingSettingsDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface IBillingService {
    InvoiceDTO getInvoice(String userId, String invoiceId);
    byte[] getInvoicePDF(String userId, String invoiceId);
    Page<InvoiceDTO> getInvoiceHistory(String userId, Pageable pageable);
    List<PaymentDTO> getPaymentHistory(String userId, int page, int size);
    InvoiceDTO createInvoice(String userId, String invoiceId, Instant startDate, Instant endDate);
    InvoiceDTO markInvoiceAsPaid(String userId, String invoiceId);
    UserBillingSettingsDocument getUserSettings(String userId);
}
