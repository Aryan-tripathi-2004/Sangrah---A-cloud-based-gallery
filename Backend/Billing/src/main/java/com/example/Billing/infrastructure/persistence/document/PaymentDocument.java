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
@Document(collection = "payments")
public class PaymentDocument {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String invoiceId;

    private PaymentMethod paymentMethod;

    private Double amount;
    private String stripePaymentIntentId;
    private String stripeChargeId;

    @Indexed
    private String status;  // SUCCESS, FAILED, PENDING

    private String failureReason;
    private Integer retryCount;
    private Instant nextRetryDate;

    private Instant transactionDate;
    private Instant createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentMethod {
        private String type;          // CREDIT_CARD
        private String last4Digits;
        private String brand;         // VISA, MASTERCARD, AMEX
        private Integer expiryMonth;
        private Integer expiryYear;
    }
}
