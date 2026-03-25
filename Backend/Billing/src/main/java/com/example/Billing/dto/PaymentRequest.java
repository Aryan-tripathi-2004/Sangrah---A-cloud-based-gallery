package com.example.Billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private String userEmail;
    private BigDecimal amount;
    private String currency;
    private String description;
    /** Stripe payment method ID (e.g. pm_card_visa obtained from the frontend) */
    private String paymentMethodId;
}
