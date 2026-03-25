package com.example.Billing.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDTO {

    private String id;
    private String invoiceId;
    private String userId;

    private BillingPeriodDTO billingPeriod;
    private StorageMetricsDTO storageMetrics;
    private ChargesDTO charges;

    private String status;  // PENDING, PAID, OVERDUE
    private Instant issuedDate;
    private Instant dueDate;
    private Instant paidDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BillingPeriodDTO {
        private Instant startDate;
        private Instant endDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageMetricsDTO {
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
    public static class ChargesDTO {
        private Double storageRate;
        private Double subtotal;
        private Double taxRate;
        private Double tax;
        private Double totalAmount;
    }
}
