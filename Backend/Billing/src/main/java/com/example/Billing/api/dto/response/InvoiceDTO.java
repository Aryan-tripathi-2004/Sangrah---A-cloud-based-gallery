package com.example.Billing.api.dto.response;

import com.example.Billing.shared.enums.InvoiceStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record InvoiceDTO(
    String id,
    String invoiceId,
    String userId,
    BillingPeriodDTO billingPeriod,
    StorageMetricsDTO storageMetrics,
    ChargesDTO charges,
    InvoiceStatus status,
    Instant issuedDate,
    Instant dueDate,
    Instant paidDate
) {
    @Builder
    public record BillingPeriodDTO(
        Instant startDate,
        Instant endDate
    ) {}

    @Builder
    public record StorageMetricsDTO(
        Double imageGBDays,
        Double imageCost,
        Double videoGBDays,
        Double videoCost,
        Double totalGBDays
    ) {}

    @Builder
    public record ChargesDTO(
        Double storageRate,
        Double subtotal,
        Double taxRate,
        Double tax,
        Double totalAmount
    ) {}
}
