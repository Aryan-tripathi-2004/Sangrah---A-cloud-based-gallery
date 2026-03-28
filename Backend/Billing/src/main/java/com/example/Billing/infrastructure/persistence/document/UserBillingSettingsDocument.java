package com.example.Billing.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_billing_settings")
public class UserBillingSettingsDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private List<SavedPaymentMethod> paymentMethods;
    private String stripeCustomerId;

    private BillingAddress billing;
    private BillingPreferences preferences;

    private Double totalPaidAllTime;
    private Double outstandingBalance;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SavedPaymentMethod {
        private String stripePaymentMethodId;
        private String last4Digits;
        private String brand;         // VISA, MASTERCARD, AMEX
        private Boolean isDefault;
        private Integer expiryMonth;
        private Integer expiryYear;
        private Instant addedDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BillingAddress {
        private String email;
        private String address;
        private String city;
        private String postalCode;
        private String country;
        private String taxId;  // VAT/GST ID
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BillingPreferences {
        private Boolean autoPayEnabled;      // Auto-retry failed payments
        private Boolean invoiceEmail;        // Email invoices
        private Double costAlertThreshold;   // Alert if monthly cost exceeds
    }
}
