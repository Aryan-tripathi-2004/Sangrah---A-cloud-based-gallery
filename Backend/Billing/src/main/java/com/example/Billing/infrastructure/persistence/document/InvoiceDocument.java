package com.example.Billing.infrastructure.persistence.document;

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
@Document(collection = "invoices")
public class InvoiceDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String invoiceId;  // e.g., "INV-2026-01-00123"

    @Indexed
    private String userId;

    private String userEmail;  // Store email for email notifications

    private BillingPeriod billingPeriod;
    private StorageMetrics storageMetrics;
    private Charges charges;

    @Indexed
    private String status;  // PENDING, PAID, OVERDUE

    private Instant issuedDate;
    private Instant dueDate;
    private Instant paidDate;

    private String paymentIntentId;  // Stripe payment intent ID

    private byte[] pdfContent;       // PDF stored as binary blob
    private Instant pdfGeneratedAt;  // When PDF was generated
    private String pdfUrl;           // Future: S3/CDN URL for PDF

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BillingPeriod {
        private Instant startDate;
        private Instant endDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageMetrics {
        private Double imageGBDays;
        private Double imageCost;
        private Double videoGBDays;
        private Double videoCost;
        private Double totalGBDays;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Charges {
        private Double storageRate;  // e.g., 0.01 per GB-day
        private Double subtotal;
        private Double taxRate;     // e.g., 0.08 for 8%
        private Double tax;
        private Double totalAmount;
    }
}
