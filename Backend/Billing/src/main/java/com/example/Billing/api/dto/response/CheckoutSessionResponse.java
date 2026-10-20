package com.example.Billing.api.dto.response;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record CheckoutSessionResponse(
    String invoiceId,
    String clientSecret,
    BigDecimal amount,
    String currency
) {}
