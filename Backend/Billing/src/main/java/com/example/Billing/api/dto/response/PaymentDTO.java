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
public class PaymentDTO {
    private String id;
    private String invoiceId;
    private Double amount;
    private String status;  // SUCCESS, FAILED, PENDING
    private Instant transactionDate;
    private String stripeChargeId;
    private String failureReason;
}
