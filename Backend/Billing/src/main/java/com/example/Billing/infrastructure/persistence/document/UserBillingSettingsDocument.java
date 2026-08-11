package com.example.Billing.infrastructure.persistence.document;

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
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_billing_settings")
public class UserBillingSettingsDocument {

    @Id
    private String id;

    @Version
    private Long version;

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

    @Getter
    @Setter
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

    @Getter
    @Setter
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BillingPreferences {
        private Boolean autoPayEnabled;      // Auto-retry failed payments
        private Boolean invoiceEmail;        // Email invoices
        private Double costAlertThreshold;   // Alert if monthly cost exceeds
    }
}
