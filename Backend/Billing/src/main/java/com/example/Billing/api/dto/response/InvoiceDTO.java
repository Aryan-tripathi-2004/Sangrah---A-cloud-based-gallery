package com.example.Billing.api.dto.response;

import java.math.BigDecimal;

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
        BigDecimal imageGBDays,
        BigDecimal imageCost,
        BigDecimal videoGBDays,
        BigDecimal videoCost,
        BigDecimal totalGBDays
    ) {}

    @Builder
    public record ChargesDTO(
        BigDecimal storageRate,
        BigDecimal subtotal,
        BigDecimal taxRate,
        BigDecimal tax,
        BigDecimal totalAmount
    ) {}
}
