package com.example.Email.api.dto;

import com.example.Email.shared.enums.EmailStatus;

import java.time.Instant;

/**
 * Immutable response record returned by all email dispatch endpoints.
 *
 * <p>Java records are inherently immutable; they replace the previous mutable
 * Lombok {@code @Data} class. The {@code status} field now carries a type-safe
 * {@link EmailStatus} enum value rather than a raw String.
 *
 * <ul>
 *   <li>{@code status}      – strongly-typed delivery status (SENT, PENDING, FAILED, BOUNCED).</li>
 *   <li>{@code emailId}     – UUID of the persisted {@code EmailLogDocument}.</li>
 *   <li>{@code timestamp}   – UTC instant at which the response was produced.</li>
 *   <li>{@code message}     – human-readable summary of the operation outcome.</li>
 *   <li>{@code errorReason} – populated only when {@code status == FAILED}; null otherwise.</li>
 * </ul>
 */
public record EmailSendResponse(

        EmailStatus status,

        String emailId,

        Instant timestamp,

        String message,

        String errorReason
) {}
