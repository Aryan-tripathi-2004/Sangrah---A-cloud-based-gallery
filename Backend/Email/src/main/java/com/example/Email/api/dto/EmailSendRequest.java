package com.example.Email.api.dto;

import com.example.Email.shared.enums.EmailType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Immutable request record carrying the payload for all email dispatch endpoints.
 *
 * <p>Java records are inherently immutable (all components are {@code final});
 * they replace the previous mutable Lombok {@code @Data} class, eliminating
 * accidental mutation between the controller and the service layer.
 *
 * <p>Jakarta validation constraints are declared directly on the record components
 * so that {@code @Valid} in the controller triggers proper constraint checking.
 *
 * <ul>
 *   <li>{@code invoiceId}   – billing reference; optional for notification emails.</li>
 *   <li>{@code userId}      – platform user identifier.</li>
 *   <li>{@code userEmail}   – validated RFC-5321 address of the recipient.</li>
 *   <li>{@code amount}      – invoice amount; optional for non-billing emails.</li>
 *   <li>{@code pdfContent}  – optional PDF attachment as a raw byte array.</li>
 *   <li>{@code emailType}   – strongly-typed {@link EmailType} enum (cures String obsession).</li>
 *   <li>{@code subject}     – email subject for generic notification emails.</li>
 *   <li>{@code body}        – email body / message for generic notification emails.</li>
 * </ul>
 */
public record EmailSendRequest(

        String invoiceId,

        String userId,

        @NotBlank(message = "Recipient email must not be blank")
        @Email(message = "A valid recipient email address is required")
        String userEmail,

        Double amount,

        byte[] pdfContent,

        @NotNull(message = "Email type must not be null")
        EmailType emailType,

        String subject,

        String body
) {}
