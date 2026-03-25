package com.example.Billing.infrastructure.persistence.repository;

import com.example.Billing.infrastructure.persistence.document.InvoiceDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends MongoRepository<InvoiceDocument, String> {

    Optional<InvoiceDocument> findByInvoiceId(String invoiceId);

    Page<InvoiceDocument> findByUserIdOrderByIssuedDateDesc(String userId, Pageable pageable);

    List<InvoiceDocument> findByUserIdAndBillingPeriod_StartDateBetween(
        String userId,
        Instant startDate,
        Instant endDate
    );

    Optional<InvoiceDocument> findByUserIdAndBillingPeriod_StartDateAndBillingPeriod_EndDate(
        String userId,
        Instant startDate,
        Instant endDate
    );

    List<InvoiceDocument> findByStatusOrderByDueDateAsc(String status);

    long countByUserIdAndStatus(String userId, String status);

    List<InvoiceDocument> findByPaymentIntentId(String paymentIntentId);
}
