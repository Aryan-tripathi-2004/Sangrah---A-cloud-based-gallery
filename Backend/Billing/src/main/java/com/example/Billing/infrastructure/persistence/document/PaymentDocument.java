package com.example.Billing.infrastructure.persistence.document;

import com.example.Billing.shared.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "payments")
public class PaymentDocument {

    @Id
    private String id;

    @Version
    private Long version;

    @Indexed
    private String userId;

    @Indexed
    private String invoiceId;

    private PaymentMethod paymentMethod;

    private Double amount;
    private String stripePaymentIntentId;
    private String stripeChargeId;

    @Indexed
    private PaymentStatus status;  // SUCCESS, FAILED, PENDING

    private String failureReason;
    private Integer retryCount;
    private Instant nextRetryDate;

    private Instant transactionDate;
    private Instant createdAt;

    @Getter
    @Setter
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
