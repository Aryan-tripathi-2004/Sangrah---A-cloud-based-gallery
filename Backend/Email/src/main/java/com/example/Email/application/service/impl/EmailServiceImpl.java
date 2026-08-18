package com.example.Email.application.service.impl;

import com.example.Email.api.dto.EmailSendResponse;
import com.example.Email.application.service.interfaces.IEmailService;
import com.example.Email.infrastructure.persistence.document.EmailLogDocument;
import com.example.Email.infrastructure.persistence.repository.EmailLogRepository;
import com.example.Email.shared.enums.EmailStatus;
import com.example.Email.shared.enums.EmailType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Concrete implementation of {@link IEmailService}.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Render Thymeleaf HTML templates.</li>
 *   <li>Dispatch emails via SMTP through {@link JavaMailSender}.</li>
 *   <li>Persist an audit trail in MongoDB via {@link EmailLogRepository}.</li>
 * </ul>
 *
 * <p><strong>Exception strategy:</strong> SMTP failures ({@link MessagingException},
 * {@link UnsupportedEncodingException}) are <em>not</em> swallowed here. They are
 * wrapped in a {@link MessagingException} (if necessary) and propagated to the
 * {@link com.example.Email.shared.exception.GlobalExceptionHandler}, which maps them
 * to RFC-9457 {@code ProblemDetail} responses. MongoDB logging failures are still
 * silently absorbed so that a logging glitch never blocks email delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailLogRepository emailLogRepository;

    @Value("${app.email.from:noreply@sangrah.com}")
    private String emailFrom;

    @Value("${app.email.from.name:Sangrah Cloud Storage}")
    private String emailFromName;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API – implements IEmailService
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Renders the {@code payment-success} Thymeleaf template, attaches the
     * PDF invoice (if provided), and sends the message. Logs the outcome to MongoDB.
     */
    @Override
    public EmailSendResponse sendInvoicePaidEmail(
            String invoiceId,
            String userId,
            String userEmail,
            Double amount,
            byte[] pdfContent
    ) throws MessagingException {

        log.info("📧 ═══════════════════════════════════════════════════════════════");
        log.info("📧 [EMAIL SERVICE] Preparing invoice-paid email");
        log.info("📧 [EMAIL SERVICE] Recipient : {}", userEmail);
        log.info("📧 [EMAIL SERVICE] Invoice   : {}", invoiceId);
        log.info("📧 [EMAIL SERVICE] Amount    : ${}", String.format("%.2f", amount));
        log.info("📧 [EMAIL SERVICE] PDF Size  : {} bytes", pdfContent != null ? pdfContent.length : 0);
        log.info("📧 [EMAIL SERVICE] Sender    : {} <{}>", emailFromName, emailFrom);
        log.info("📧 ═══════════════════════════════════════════════════════════════");

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("invoiceId", invoiceId);
            variables.put("amount", String.format("$%.2f", amount));
            variables.put("paidDate", Instant.now().toString());
            variables.put("downloadLink", "https://app.sangrah.com/invoices/" + invoiceId + "/download");

            log.info("📝 [EMAIL SERVICE] Rendering payment-success template…");
            String htmlContent = renderTemplate("payment-success", variables);
            log.info("✅ [EMAIL SERVICE] Template rendered successfully");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailFrom, emailFromName);
            helper.setTo(userEmail);
            helper.setSubject("Payment Confirmation - Invoice " + invoiceId);
            helper.setText(htmlContent, true);

            if (pdfContent != null && pdfContent.length > 0) {
                helper.addAttachment("invoice_" + invoiceId + ".pdf",
                        () -> new java.io.ByteArrayInputStream(pdfContent));
                log.info("📎 [EMAIL SERVICE] PDF attachment added: {} bytes", pdfContent.length);
            }

            log.info("🚀 [EMAIL SERVICE] Sending email via SMTP…");
            mailSender.send(message);
            log.info("✅ [EMAIL SERVICE] EMAIL SENT SUCCESSFULLY!");

            String emailLogId = UUID.randomUUID().toString();
            logEmailDelivery(emailLogId, userEmail, EmailType.INVOICE_PAID, EmailStatus.SENT, invoiceId);
            log.info("✅ [EMAIL SERVICE] Email logged: {}", emailLogId);

            return new EmailSendResponse(
                    EmailStatus.SENT,
                    emailLogId,
                    Instant.now(),
                    "Payment confirmation email sent successfully to: " + userEmail,
                    null
            );

        } catch (UnsupportedEncodingException e) {
            // Wrap checked UnsupportedEncodingException as MessagingException so the
            // single exception type propagates cleanly to the GlobalExceptionHandler.
            throw new MessagingException("Unsupported encoding while building invoice-paid email", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Renders the {@code invoice-created} Thymeleaf template and dispatches the email.
     */
    @Override
    public EmailSendResponse sendInvoiceCreatedEmail(
            String invoiceId,
            String userId,
            String userEmail,
            Double amount,
            String dueDate
    ) throws MessagingException {

        log.info("📧 [EMAIL SERVICE] Sending invoice-created email to: {}", userEmail);

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("invoiceId", invoiceId);
            variables.put("amount", String.format("$%.2f", amount));
            variables.put("dueDate", dueDate);
            variables.put("payLink", "https://app.sangrah.com/invoices/" + invoiceId + "/pay");

            String htmlContent = renderTemplate("invoice-created", variables);
            sendHtmlEmail(userEmail, "Invoice " + invoiceId + " - Payment Due", htmlContent);

            String emailLogId = UUID.randomUUID().toString();
            log.info("✅ [EMAIL SERVICE] Invoice-created email sent: {}", emailLogId);
            logEmailDelivery(emailLogId, userEmail, EmailType.INVOICE_CREATED, EmailStatus.SENT, invoiceId);

            return new EmailSendResponse(
                    EmailStatus.SENT,
                    emailLogId,
                    Instant.now(),
                    "Invoice created email sent successfully",
                    null
            );

        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Unsupported encoding while building invoice-created email", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Renders the {@code payment-failed} Thymeleaf template and dispatches the email.
     */
    @Override
    public EmailSendResponse sendPaymentFailedEmail(
            String invoiceId,
            String userId,
            String userEmail,
            String failureReason
    ) throws MessagingException {

        log.info("📧 [EMAIL SERVICE] Sending payment-failed email to: {}", userEmail);

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("invoiceId", invoiceId);
            variables.put("failureReason", failureReason);
            variables.put("retryLink", "https://app.sangrah.com/invoices/" + invoiceId + "/retry-payment");
            variables.put("supportEmail", "support@sangrah.com");

            String htmlContent = renderTemplate("payment-failed", variables);
            sendHtmlEmail(userEmail, "Payment Failed - Invoice " + invoiceId, htmlContent);

            String emailLogId = UUID.randomUUID().toString();
            log.info("✅ [EMAIL SERVICE] Payment-failed email sent: {}", emailLogId);
            logEmailDelivery(emailLogId, userEmail, EmailType.PAYMENT_FAILED, EmailStatus.SENT, invoiceId);

            return new EmailSendResponse(
                    EmailStatus.SENT,
                    emailLogId,
                    Instant.now(),
                    "Payment failed notification sent successfully",
                    null
            );

        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Unsupported encoding while building payment-failed email", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds a self-contained HTML notification body (no external template required)
     * and dispatches it. The {@code notificationType} string is resolved to an
     * {@link EmailType} enum constant; unrecognised values fall back to
     * {@link EmailType#NOTIFICATION}.
     */
    @Override
    public EmailSendResponse sendNotificationEmail(
            String userEmail,
            String notificationType,
            String subject,
            String messageBody
    ) throws MessagingException {

        log.info("📧 [EMAIL SERVICE] Sending notification email to: {} | type: {}", userEmail, notificationType);

        try {
            String emailLogId = UUID.randomUUID().toString();

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(emailFrom, emailFromName);
            helper.setTo(userEmail);
            helper.setSubject(subject);

            String htmlBody = String.format(
                    "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>"
                    + "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>"
                    + "<h2 style='color: #007bff;'>%s</h2>"
                    + "<p>%s</p>"
                    + "<p style='margin-top: 30px; font-size: 12px; color: #666;'>"
                    + "This is an automated notification from Sangrah Cloud Storage. "
                    + "Please do not reply to this email."
                    + "</p>"
                    + "</div>"
                    + "</body></html>",
                    subject,
                    messageBody
            );

            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
            log.info("✅ [EMAIL SERVICE] Notification email sent to: {}", userEmail);

            // Resolve string to enum; fall back gracefully for unknown event types.
            EmailType resolvedType = resolveEmailType(notificationType);
            logEmailDelivery(emailLogId, userEmail, resolvedType, EmailStatus.SENT, null);

            return new EmailSendResponse(
                    EmailStatus.SENT,
                    emailLogId,
                    Instant.now(),
                    "Notification email sent successfully",
                    null
            );

        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Unsupported encoding while building notification email", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Send a pre-rendered HTML email without an attachment.
     */
    private void sendHtmlEmail(String toEmail, String subject, String htmlContent)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(emailFrom, emailFromName);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    /**
     * Render a Thymeleaf template with the supplied variable map.
     *
     * @param templateName logical name matching a {@code .html} file under
     *                     {@code src/main/resources/templates/}
     * @param variables    key-value pairs injected into the template context
     * @return rendered HTML string
     */
    private String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        return templateEngine.process(templateName, context);
    }

    /**
     * Persist an email dispatch audit record in MongoDB.
     *
     * <p>Logging failures are deliberately swallowed – a MongoDB outage must
     * never prevent an email from being sent.
     */
    private void logEmailDelivery(
            String emailId,
            String toEmail,
            EmailType emailType,
            EmailStatus status,
            String invoiceId
    ) {
        try {
            EmailLogDocument emailLog = EmailLogDocument.builder()
                    .emailId(emailId)
                    .toEmail(toEmail)
                    .fromEmail(emailFrom)
                    .emailType(emailType)
                    .invoiceId(invoiceId)
                    .status(status)
                    .sentAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();

            emailLogRepository.save(emailLog);
            log.info("📝 [EMAIL SERVICE] Email delivery logged: {}", emailId);
        } catch (Exception ex) {
            log.error("❌ [EMAIL SERVICE] Failed to log email delivery for {}: {}", emailId, ex.getMessage(), ex);
            // Intentionally not re-thrown – logging must not block email dispatch.
        }
    }

    /**
     * Resolve a notification-type string (coming from external Event Service)
     * to a strongly-typed {@link EmailType} enum constant.
     *
     * <p>The comparison is case-insensitive and underscore/hyphen agnostic.
     * If no match is found, {@link EmailType#NOTIFICATION} is returned as the
     * safe default so that the log entry is never {@code null}.
     *
     * @param notificationType raw string from the caller
     * @return the best-matching {@link EmailType} constant
     */
    private EmailType resolveEmailType(String notificationType) {
        if (notificationType == null || notificationType.isBlank()) {
            return EmailType.NOTIFICATION;
        }
        String normalised = notificationType.toUpperCase().replace("-", "_");
        try {
            return EmailType.valueOf(normalised);
        } catch (IllegalArgumentException ex) {
            log.warn("⚠️ [EMAIL SERVICE] Unknown email type '{}'; defaulting to NOTIFICATION", notificationType);
            return EmailType.NOTIFICATION;
        }
    }
}
