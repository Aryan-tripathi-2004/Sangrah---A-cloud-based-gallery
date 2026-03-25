package com.example.Email.infrastructure.persistence.repository;

import com.example.Email.infrastructure.persistence.document.EmailLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailLogRepository extends MongoRepository<EmailLogDocument, String> {

    Optional<EmailLogDocument> findByEmailId(String emailId);

    List<EmailLogDocument> findByToEmail(String toEmail);

    List<EmailLogDocument> findByStatus(String status);

    List<EmailLogDocument> findByInvoiceId(String invoiceId);

    List<EmailLogDocument> findByEmailType(String emailType);

    List<EmailLogDocument> findByStatusAndCreatedAtAfter(String status, Instant createdAfter);

    List<EmailLogDocument> findByToEmailAndEmailType(String toEmail, String emailType);
}
