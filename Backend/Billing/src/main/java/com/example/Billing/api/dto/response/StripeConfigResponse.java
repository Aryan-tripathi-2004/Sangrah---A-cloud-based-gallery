package com.example.Billing.api.dto.response;

import lombok.Builder;

@Builder
public record StripeConfigResponse(
    String stripePublishableKey,
    Boolean stripeConfigured,
    String message
) {}
