package com.example.Billing.api.dto.response;

import com.example.Billing.shared.enums.PaymentStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record PaymentDTO(
    String id,
    String invoiceId,
    Double amount,
    PaymentStatus status,
    Instant transactionDate,
    String stripeChargeId,
    String failureReason
) {}
