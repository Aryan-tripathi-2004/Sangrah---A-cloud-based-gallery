package com.example.Billing.repository;

import com.example.Billing.model.Invoice;
import com.example.Billing.model.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUserEmail(String userEmail);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Optional<Invoice> findByStripePaymentIntentId(String paymentIntentId);
    List<Invoice> findByStatus(InvoiceStatus status);
}
