package com.example.Email.application.service.interfaces;

import com.example.Email.api.dto.EmailSendRequest;
import com.example.Email.api.dto.EmailSendResponse;
import com.example.Email.api.dto.response.EmailServiceInfoResponse;
import com.example.Email.api.dto.response.HealthResponse;
import com.example.Email.shared.enums.EmailStatus;
import com.example.Email.shared.enums.EmailType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

/**
 * Service contract for all email dispatch operations within the Sangrah platform.
 */
public interface IEmailService {

    /**
     * Send a payment-confirmation email with an optional PDF invoice attachment.
     */
    EmailSendResponse sendInvoicePaidEmail(
            String invoiceId,
            String userId,
            String userEmail,
            BigDecimal amount,
            byte[] pdfContent
    );

    /**
     * Send a new-invoice notification with a payment link.
     */
    EmailSendResponse sendInvoiceCreatedEmail(
            String invoiceId,
            String userId,
            String userEmail,
            BigDecimal amount,
            String dueDate
    );

    /**
     * Send a payment-failure alert with a retry link.
     */
    EmailSendResponse sendPaymentFailedEmail(
            String invoiceId,
            String userId,
            String userEmail,
            String failureReason
    );

    /**
     * Send a generic platform notification email.
     */
    EmailSendResponse sendNotificationEmail(
            String userEmail,
            EmailType notificationType,
            String subject,
            String messageBody
    );

    default EmailSendResponse sendNotificationEmail(
            String userId,
            String userEmail,
            String subject,
            String body
    ) {
        return sendNotificationEmail(userEmail, EmailType.NOTIFICATION, subject, body);
    }

    /**
     * Asynchronously dispatch an HTML email offloaded to a background thread pool.
     */
    void sendHtmlEmailAsync(String toEmail, String subject, String htmlContent);

    /**
     * Polymorphic dispatcher that routes an incoming {@link EmailSendRequest} to
     * the appropriate specialised method based on {@link EmailType}.
     */
    EmailSendResponse dispatchEmail(EmailSendRequest request);

    default EmailSendResponse sendEmail(EmailSendRequest request) {
        return dispatchEmail(request);
    }

    default ResponseEntity<EmailSendResponse> dispatchEmailResponse(EmailSendRequest request) {
        EmailSendResponse response = dispatchEmail(request);
        HttpStatus status = (response != null && EmailStatus.SENT.equals(response.status())) ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Retrieve runtime operational metadata about this service instance.
     */
    EmailServiceInfoResponse getServiceInfo();

    String getHealth();

    default HealthResponse getHealthResponse() {
        return new HealthResponse(getHealth());
    }
}
