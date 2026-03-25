package com.example.Email.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "email_logs")
public class EmailLogDocument {

    @Id
    private String id;

    @Indexed
    private String emailId;  // Unique email identifier

    @Indexed
    private String toEmail;  // Recipient email

    private String fromEmail;  // Sender email

    @Indexed
    private String emailType;  // invoice-paid, invoice-created, payment-failed, otp, etc.

    @Indexed
    private String invoiceId;  // Associated invoice (if applicable)

    @Indexed
    private String status;  // sent, pending, failed, bounced

    private Instant sentAt;  // When email was sent

    private Instant deliveredAt;  // When email was delivered (from provider)

    private String failureReason;  // Why email failed (if applicable)

    private Integer retryCount;  // Number of retry attempts

    private Instant lastRetryAt;  // Last retry timestamp

    private Instant createdAt;  // When log was created
}
