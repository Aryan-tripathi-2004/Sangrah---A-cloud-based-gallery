package com.example.Billing.infrastructure.event;

import java.math.BigDecimal;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * Event: Invoice Paid
 *
 * Published: When payment is successfully completed (via Stripe webhook)
 * Listeners:
 *   1. Notification service subscribes to send payment confirmation email
 *   2. Billing service PDF generator auto-generates invoice PDF
 *
 * Usage:
 * eventPublisher.publishEvent(new InvoicePaidEvent(
 *     this, invoiceId, userId, totalAmount
 * ));
 */
@Getter
public class InvoicePaidEvent extends ApplicationEvent {
    private final String invoiceId;
    private final String userId;
    private final BigDecimal totalAmount;
    private final Instant paidDate;

    public InvoicePaidEvent(Object source, String invoiceId, String userId,
                            BigDecimal totalAmount) {
        super(source);
        this.invoiceId = invoiceId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.paidDate = Instant.now();
    }
}
