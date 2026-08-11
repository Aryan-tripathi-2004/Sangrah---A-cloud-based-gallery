package com.example.Billing.infrastructure.persistence.repository;

import com.example.Billing.infrastructure.persistence.document.PaymentDocument;
import com.example.Billing.shared.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<PaymentDocument, String> {

    Optional<PaymentDocument> findByStripePaymentIntentId(String stripePaymentIntentId);

    Page<PaymentDocument> findByUserIdOrderByTransactionDateDesc(String userId, Pageable pageable);

    List<PaymentDocument> findByUserIdOrderByTransactionDateDesc(String userId);

    List<PaymentDocument> findByInvoiceIdOrderByTransactionDateDesc(String invoiceId);

    List<PaymentDocument> findByStatusAndNextRetryDateLessThanEqualOrderByNextRetryDateAsc(
        PaymentStatus status,
        Instant dateTime
    );

    long countByUserIdAndStatus(String userId, PaymentStatus status);
}
