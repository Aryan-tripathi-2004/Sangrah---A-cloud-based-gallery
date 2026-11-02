package com.example.Email.application.service.impl;

import com.example.Email.api.dto.EmailSendRequest;
import com.example.Email.api.dto.EmailSendResponse;
import com.example.Email.api.dto.response.EmailServiceInfoResponse;
import com.example.Email.application.service.interfaces.IAsyncEmailSender;
import com.example.Email.application.service.interfaces.IEmailService;
import com.example.Email.infrastructure.persistence.document.EmailLogDocument;
import com.example.Email.infrastructure.persistence.repository.EmailLogRepository;
import com.example.Email.shared.enums.EmailStatus;
import com.example.Email.shared.enums.EmailType;
import com.example.Email.shared.exception.EmailProcessingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Concrete implementation of {@link IEmailService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailLogRepository emailLogRepository;
    private final IAsyncEmailSender asyncEmailSender;

    @Value("${app.email.from}")
    private String emailFrom;

    @Value("${app.email.from.name}")
    private String emailFromName;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.email.support}")
    private String supportEmail;

    @Override
    public EmailSendResponse dispatchEmail(EmailSendRequest request) {
        return switch (request.emailType()) {
            case INVOICE_PAID -> sendInvoicePaidEmail(
                    request.invoiceId(), request.userId(), request.userEmail(), request.amount(), request.pdfContent());
            case INVOICE_CREATED -> sendInvoiceCreatedEmail(
                    request.invoiceId(), request.userId(), request.userEmail(), request.amount(), "Invoice Due");
            case PAYMENT_FAILED -> sendPaymentFailedEmail(
                    request.invoiceId(), request.userId(), request.userEmail(), "Payment processing failed");
            default -> sendNotificationEmail(
                    request.userEmail(), request.emailType(), request.subject(), request.body());
        };
    }

    @Override
    public EmailSendResponse sendInvoicePaidEmail(
            String invoiceId,
            String userId,
            String userEmail,
            BigDecimal amount,
            byte[] pdfContent
    ) {
        log.info("📧 [EMAIL SERVICE] Sending payment confirmation email to: {}", userEmail);

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("invoiceId", invoiceId);
            variables.put("amount", amount != null ? String.format(Locale.US, "$%.2f", amount) : "$0.00");
            variables.put("paymentDate", Instant.now().toString());

            String htmlContent = renderTemplate("payment-success", variables);
            String emailLogId = UUID.randomUUID().toString();
            String subject = "Payment Confirmation - Invoice " + invoiceId;
            String attachmentFilename = "invoice-" + invoiceId + ".pdf";

            logEmailDelivery(emailLogId, userEmail, EmailType.INVOICE_PAID, EmailStatus.PENDING, invoiceId,
                    subject, htmlContent, attachmentFilename, pdfContent);

            asyncEmailSender.sendEmailWithAttachmentAsync(
                    emailLogId,
                    emailFrom,
                    emailFromName,
                    userEmail,
                    subject,
                    htmlContent,
                    attachmentFilename,
                    pdfContent
            );

            log.info("✅ [EMAIL SERVICE] Payment confirmation email offloaded for background delivery: {}", emailLogId);

            return new EmailSendResponse(
                    EmailStatus.PENDING,
                    emailLogId,
                    Instant.now(),
                    "Payment confirmation email queued for delivery to: " + userEmail,
                    null
            );

        } catch (Exception e) {
            throw new EmailProcessingException("Failed to send invoice-paid email", e);
        }
    }

    @Override
    public EmailSendResponse sendInvoiceCreatedEmail(
            String invoiceId,
            String userId,
            String userEmail,
            BigDecimal amount,
            String dueDate
    ) {
        log.info("📧 [EMAIL SERVICE] Sending invoice-created email to: {}", userEmail);

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("invoiceId", invoiceId);
            variables.put("amount", amount != null ? String.format(Locale.US, "$%.2f", amount) : "$0.00");
            variables.put("dueDate", dueDate);
            variables.put("payLink", frontendUrl + "/invoices/" + invoiceId + "/pay");

            String htmlContent = renderTemplate("invoice-created", variables);
            String emailLogId = UUID.randomUUID().toString();
            String subject = "Invoice " + invoiceId + " - Payment Due";
            logEmailDelivery(emailLogId, userEmail, EmailType.INVOICE_CREATED, EmailStatus.PENDING, invoiceId,
                    subject, htmlContent, null, null);

            asyncEmailSender.sendHtmlEmailAsync(
                    emailLogId,
                    emailFrom,
                    emailFromName,
                    userEmail,
                    subject,
                    htmlContent
            );

            log.info("✅ [EMAIL SERVICE] Invoice-created email offloaded for background delivery: {}", emailLogId);

            return new EmailSendResponse(
                    EmailStatus.PENDING,
                    emailLogId,
                    Instant.now(),
                    "Invoice created email queued for delivery",
                    null
            );

        } catch (Exception e) {
            throw new EmailProcessingException("Failed to send invoice-created email", e);
        }
    }

    @Override
    public EmailSendResponse sendPaymentFailedEmail(
            String invoiceId,
            String userId,
            String userEmail,
            String failureReason
    ) {
        log.info("📧 [EMAIL SERVICE] Sending payment-failed alert to: {}", userEmail);

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("invoiceId", invoiceId);
            variables.put("failureReason", failureReason != null ? failureReason : "Card declined");
            variables.put("retryLink", frontendUrl + "/invoices/" + invoiceId + "/pay");

            String htmlContent = renderTemplate("payment-failed", variables);
            String emailLogId = UUID.randomUUID().toString();
            String subject = "Payment Failed - Invoice " + invoiceId;
            logEmailDelivery(emailLogId, userEmail, EmailType.PAYMENT_FAILED, EmailStatus.PENDING, invoiceId,
                    subject, htmlContent, null, null);

            asyncEmailSender.sendHtmlEmailAsync(
                    emailLogId,
                    emailFrom,
                    emailFromName,
                    userEmail,
                    subject,
                    htmlContent
            );

            log.info("✅ [EMAIL SERVICE] Payment-failed email offloaded for background delivery: {}", emailLogId);

            return new EmailSendResponse(
                    EmailStatus.PENDING,
                    emailLogId,
                    Instant.now(),
                    "Payment failed notification queued for delivery",
                    null
            );

        } catch (Exception e) {
            throw new EmailProcessingException("Failed to send payment-failed email", e);
        }
    }

    @Override
    public EmailSendResponse sendNotificationEmail(
            String userEmail,
            EmailType notificationType,
            String subject,
            String messageBody
    ) {
        log.info("📧 [EMAIL SERVICE] Sending notification email to: {} | type: {}", userEmail, notificationType.name());

        try {
            String emailLogId = UUID.randomUUID().toString();

            Map<String, Object> variables = new HashMap<>();
            variables.put("subject", subject);
            variables.put("messageBody", messageBody);

            String htmlContent = renderTemplate("generic-notification", variables);
            logEmailDelivery(emailLogId, userEmail, notificationType, EmailStatus.PENDING, null,
                    subject, htmlContent, null, null);

            asyncEmailSender.sendHtmlEmailAsync(
                    emailLogId,
                    emailFrom,
                    emailFromName,
                    userEmail,
                    subject,
                    htmlContent
            );

            log.info("✅ [EMAIL SERVICE] Notification email offloaded for background delivery: {}", emailLogId);

            return new EmailSendResponse(
                    EmailStatus.PENDING,
                    emailLogId,
                    Instant.now(),
                    "Notification email queued for delivery to: " + userEmail,
                    null
            );

        } catch (Exception e) {
            throw new EmailProcessingException("Failed to send notification email", e);
        }
    }

    @Override
    public void sendHtmlEmailAsync(String toEmail, String subject, String htmlContent) {
        String logId = java.util.UUID.randomUUID().toString();
        logEmailDelivery(logId, toEmail, EmailType.NOTIFICATION, EmailStatus.PENDING, null, subject, htmlContent, null, null);
        asyncEmailSender.sendHtmlEmailAsync(logId, emailFrom, emailFromName, toEmail, subject, htmlContent);
    }

    private String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        return templateEngine.process(templateName, context);
    }

    private void logEmailDelivery(
            String emailId,
            String toEmail,
            EmailType emailType,
            EmailStatus status,
            String invoiceId,
            String subject,
            String htmlContent,
            String attachmentFilename,
            byte[] attachmentBytes
    ) {
        try {
            EmailLogDocument emailLog = EmailLogDocument.builder()
                    .emailId(emailId)
                    .toEmail(toEmail)
                    .fromEmail(emailFrom)
                    .emailType(emailType)
                    .invoiceId(invoiceId)
                    .status(status)
                    .subject(subject)
                    .htmlContent(htmlContent)
                    .attachmentFilename(attachmentFilename)
                    .attachmentBytes(attachmentBytes)
                    .retryCount(0)
                    .sentAt(status == EmailStatus.SENT ? Instant.now() : null)
                    .createdAt(Instant.now())
                    .build();

            emailLogRepository.save(emailLog);
            log.info("📝 [EMAIL SERVICE] Email delivery logged: {}", emailId);
        } catch (Exception ex) {
            log.error("❌ [EMAIL SERVICE] Failed to log email delivery for {}: {}", emailId, ex.getMessage(), ex);
        }
    }

    @Override
    public EmailServiceInfoResponse getServiceInfo() {
        return EmailServiceInfoResponse.defaults();
    }

    @Override
    public String getHealth() {
        return "Email service is running";
    }
}
