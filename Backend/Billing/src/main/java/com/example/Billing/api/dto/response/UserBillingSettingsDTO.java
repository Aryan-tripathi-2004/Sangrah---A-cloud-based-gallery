package com.example.Billing.api.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record UserBillingSettingsDTO(
        String id,
        String userId,
        String stripeCustomerId,
        BigDecimal totalPaidAllTime,
        BigDecimal outstandingBalance,
        Boolean autoPayEnabled,
        Boolean invoiceEmail,
        BigDecimal costAlertThreshold,
        Instant createdAt,
        Instant updatedAt
) {}
