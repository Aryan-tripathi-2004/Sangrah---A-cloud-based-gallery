package com.example.Billing.application.service.interfaces;

import com.example.Billing.api.dto.response.InvoiceDTO;
import com.example.Billing.api.dto.response.PaymentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

/**
 * Service contract for managing the lifecycle, persistence, and state transitions of Invoices.
 * Encapsulates invoice creation, PDF retrieval, payment history, and status updates.
 */
public interface IInvoiceLifecycleService {

    /**
     * Retrieves invoice details for a specific user and invoice ID.
     *
     * @param userId user identifier
     * @param invoiceId invoice identifier
     * @return invoice DTO
     */
    InvoiceDTO getInvoice(String userId, String invoiceId);

    /**
     * Retrieves the raw PDF byte content for an invoice, generating it on-demand if missing.
     *
     * @param userId user identifier
     * @param invoiceId invoice identifier
     * @return raw PDF byte array
     */
    byte[] getInvoicePDF(String userId, String invoiceId);

    /**
     * Retrieves paginated invoice history for a user.
     *
     * @param userId user identifier
     * @param pageable pagination parameters
     * @return page of invoice DTOs
     */
    Page<InvoiceDTO> getInvoiceHistory(String userId, Pageable pageable);

    /**
     * Retrieves payment history for a user.
     *
     * @param userId user identifier
     * @param page page index
     * @param size page size
     * @return list of payment DTOs
     */
    List<PaymentDTO> getPaymentHistory(String userId, int page, int size);

    /**
     * Creates and persists a new invoice for a user and billing period.
     *
     * @param userId user identifier
     * @param invoiceId unique invoice ID
     * @param startDate start of billing period
     * @param endDate end of billing period
     * @return created invoice DTO
     */
    InvoiceDTO createInvoice(String userId, String invoiceId, Instant startDate, Instant endDate);

    /**
     * Marks an existing invoice as paid.
     *
     * @param userId user identifier
     * @param invoiceId invoice identifier
     * @return updated invoice DTO
     */
    InvoiceDTO markInvoiceAsPaid(String userId, String invoiceId);
}
