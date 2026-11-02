package com.example.Billing.api.dto.response;

import lombok.Builder;
import java.time.Instant;

@Builder
public record PaymentStatusResponse(
    String invoiceId,
    String status,
    Instant paidDate
) {}
