package com.example.Billing.infrastructure.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * Event: Payment Failed
 *
 * Published: When Stripe payment attempt fails (e.g., card declined)
 * Listeners: Notification service subscribes to send failure email with retry options
 *
 * Usage:
 * eventPublisher.publishEvent(new PaymentFailedEvent(
 *     this, invoiceId, userId, failureReason, retryCount
 * ));
 */
@Getter
public class PaymentFailedEvent extends ApplicationEvent {
    private final String invoiceId;
    private final String userId;
    private final String failureReason;
    private final Integer retryCount;
    private final Instant failedAt;

    public PaymentFailedEvent(Object source, String invoiceId, String userId,
                              String failureReason, Integer retryCount) {
        super(source);
        this.invoiceId = invoiceId;
        this.userId = userId;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
        this.failedAt = Instant.now();
    }
}
