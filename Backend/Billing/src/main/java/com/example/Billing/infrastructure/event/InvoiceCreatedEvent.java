package com.example.Billing.infrastructure.event;

import java.math.BigDecimal;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * Event: Invoice Created
 *
 * Published: When a new invoice is generated (via batch job)
 * Listeners: Notification service subscribes to send email
 *
 * Usage:
 * eventPublisher.publishEvent(new InvoiceCreatedEvent(
 *     this, invoiceId, userId, invoiceNumber, totalAmount
 * ));
 */
@Getter
public class InvoiceCreatedEvent extends ApplicationEvent {
    private final String invoiceId;
    private final String userId;
    private final String invoiceNumber;
    private final BigDecimal totalAmount;
    private final Instant createdAt;

    public InvoiceCreatedEvent(Object source, String invoiceId, String userId,
                               String invoiceNumber, BigDecimal totalAmount) {
        super(source);
        this.invoiceId = invoiceId;
        this.userId = userId;
        this.invoiceNumber = invoiceNumber;
        this.totalAmount = totalAmount;
        this.createdAt = Instant.now();
    }
}
