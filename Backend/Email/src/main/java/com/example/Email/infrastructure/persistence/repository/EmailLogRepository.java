package com.example.Email.infrastructure.persistence.repository;

import com.example.Email.infrastructure.persistence.document.EmailLogDocument;
import com.example.Email.shared.enums.EmailStatus;
import com.example.Email.shared.enums.EmailType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link EmailLogDocument}.
 *
 * <p>All query methods are typed against the {@link EmailStatus} and
 * {@link EmailType} enums – eliminating the primitive String obsession
 * that previously existed on the {@code status} and {@code emailType} fields.
 */
@Repository
public interface EmailLogRepository extends MongoRepository<EmailLogDocument, String> {

    Optional<EmailLogDocument> findByEmailId(String emailId);

    List<EmailLogDocument> findByToEmail(String toEmail);

    /** Find all logs matching a given delivery status. */
    List<EmailLogDocument> findByStatus(EmailStatus status);

    List<EmailLogDocument> findByInvoiceId(String invoiceId);

    /** Find all logs matching a given email category. */
    List<EmailLogDocument> findByEmailType(EmailType emailType);

    /**
     * Find logs by status created after a given point in time.
     * Useful for retry-worker queries (e.g. all PENDING emails in the last hour).
     */
    List<EmailLogDocument> findByStatusAndCreatedAtAfter(EmailStatus status, Instant createdAfter);

    List<EmailLogDocument> findByToEmailAndEmailType(String toEmail, EmailType emailType);
}
