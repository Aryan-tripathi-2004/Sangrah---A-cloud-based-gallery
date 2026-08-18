package com.example.Email.infrastructure.persistence.document;

import com.example.Email.shared.enums.EmailStatus;
import com.example.Email.shared.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document that persists an audit log for every email dispatch attempt.
 *
 * <p>Optimistic locking is enabled via {@code @Version} to prevent lost-update
 * anomalies when multiple threads concurrently update the same log entry
 * (e.g., a retry worker and the initial sender).
 *
 * <p>Lombok's broad {@code @Data} has been replaced with granular annotations
 * to avoid generating {@code equals}/{@code hashCode} based on mutable state
 * and to make intentionality explicit.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "email_logs")
public class EmailLogDocument {

    @Id
    private String id;

    @Indexed
    private String emailId;           // Unique email identifier

    @Indexed
    private String toEmail;           // Recipient email

    private String fromEmail;         // Sender email

    @Indexed
    private EmailType emailType;      // Strongly-typed category (was raw String)

    @Indexed
    private String invoiceId;         // Associated invoice (if applicable)

    @Indexed
    private EmailStatus status;       // Strongly-typed status (was raw String)

    private Instant sentAt;           // When email was sent

    private Instant deliveredAt;      // When email was delivered (from provider)

    private String failureReason;     // Why email failed (if applicable)

    private Integer retryCount;       // Number of retry attempts

    private Instant lastRetryAt;      // Last retry timestamp

    private Instant createdAt;        // When log was created

    /**
     * Optimistic-locking token managed exclusively by Spring Data MongoDB.
     * Do NOT set this field manually; it is incremented on every successful save.
     */
    @Version
    private Long version;
}
