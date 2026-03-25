package com.example.Billing.dto;

import com.example.Billing.model.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private String userEmail;
    private BigDecimal amount;
    private String currency;
    private InvoiceStatus status;
    private String stripePaymentIntentId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
