package com.example.Billing.api.dto.response;

import java.math.BigDecimal;

import com.example.Billing.shared.enums.PaymentStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record PaymentDTO(
    String id,
    String invoiceId,
    BigDecimal amount,
    PaymentStatus status,
    Instant transactionDate,
    String stripeChargeId,
    String failureReason
) {}
