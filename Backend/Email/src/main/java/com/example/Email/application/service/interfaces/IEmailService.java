package com.example.Email.application.service.interfaces;

import com.example.Email.api.dto.EmailSendRequest;
import com.example.Email.api.dto.EmailSendResponse;

import jakarta.mail.MessagingException;

/**
 * Service contract for all email dispatch operations within the Sangrah platform.
 *
 * <p>Declaring this interface in the {@code application} layer and depending on it
 * from the {@code api} layer satisfies the Dependency Inversion Principle (DIP):
 * high-level policy ({@code EmailSendController}) depends on an abstraction, not on
 * a concrete Spring {@code @Service} bean.
 *
 * <p>All methods propagate {@link MessagingException} so that the
 * {@link com.example.Email.shared.exception.GlobalExceptionHandler} can translate
 * SMTP failures into RFC-9457 {@code ProblemDetail} responses without any
 * try-catch boilerplate in the controller.
 */
public interface IEmailService {

    /**
     * Send a payment-confirmation email with an optional PDF invoice attachment.
     *
     * @param invoiceId  billing reference identifier
     * @param userId     platform user identifier
     * @param userEmail  validated recipient address
     * @param amount     invoice total in USD
     * @param pdfContent raw bytes of the PDF attachment, or {@code null}
     * @return an immutable {@link EmailSendResponse} describing the outcome
     * @throws MessagingException if the SMTP transport layer rejects the message
     */
    EmailSendResponse sendInvoicePaidEmail(
            String invoiceId,
            String userId,
            String userEmail,
            Double amount,
            byte[] pdfContent
    ) throws MessagingException;

    /**
     * Send a new-invoice notification with a payment link.
     *
     * @param invoiceId billing reference identifier
     * @param userId    platform user identifier
     * @param userEmail validated recipient address
     * @param amount    invoice total in USD
     * @param dueDate   human-readable due date string
     * @return an immutable {@link EmailSendResponse} describing the outcome
     * @throws MessagingException if the SMTP transport layer rejects the message
     */
    EmailSendResponse sendInvoiceCreatedEmail(
            String invoiceId,
            String userId,
            String userEmail,
            Double amount,
            String dueDate
    ) throws MessagingException;

    /**
     * Send a payment-failure alert with a retry link.
     *
     * @param invoiceId     billing reference identifier
     * @param userId        platform user identifier
     * @param userEmail     validated recipient address
     * @param failureReason human-readable reason for the failure
     * @return an immutable {@link EmailSendResponse} describing the outcome
     * @throws MessagingException if the SMTP transport layer rejects the message
     */
    EmailSendResponse sendPaymentFailedEmail(
            String invoiceId,
            String userId,
            String userEmail,
            String failureReason
    ) throws MessagingException;

    /**
     * Send a generic platform notification email.
     * Used by the Event Service for access-approved, access-rejected, media-approved, etc.
     *
     * @param userEmail        validated recipient address
     * @param notificationType raw event type string (mapped internally to {@link com.example.Email.shared.enums.EmailType})
     * @param subject          email subject line
     * @param messageBody      HTML or plain-text email body
     * @return an immutable {@link EmailSendResponse} describing the outcome
     * @throws MessagingException if the SMTP transport layer rejects the message
     */
    EmailSendResponse sendNotificationEmail(
            String userEmail,
            String notificationType,
            String subject,
            String messageBody
    ) throws MessagingException;
}
