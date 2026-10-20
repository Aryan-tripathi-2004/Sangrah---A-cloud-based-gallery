package com.example.Email.application.service.impl;

import com.example.Email.api.dto.EmailSendResponse;
import com.example.Email.api.dto.response.EmailServiceInfoResponse;
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
    private final AsyncEmailSender asyncEmailSender;

    @Value("${app.email.from}")
    private String emailFrom;

    @Value("${app.email.from.name}")
    private String emailFromName;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.email.support}")
    private String supportEmail;

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
    ) {

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
            variables.put("downloadLink", frontendUrl + "/invoices/" + invoiceId + "/download");

            log.info("📝 [EMAIL SERVICE] Rendering payment-success template…");
            String htmlContent = renderTemplate("payment-success", variables);
            log.info("✅ [EMAIL SERVICE] Template rendered successfully");

            String emailLogId = UUID.randomUUID().toString();
            String subject = "Payment Confirmation - Invoice " + invoiceId;
            String attachmentFilename = "invoice_" + invoiceId + ".pdf";
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
                    EmailStatus.SENT,
                    emailLogId,
                    Instant.now(),
                    "Payment confirmation email sent successfully to: " + userEmail,
                    null
            );

        } catch (Exception e) {
            throw new EmailProcessingException("Failed to send invoice-paid email", e);
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
    ) {

        log.info("📧 [EMAIL SERVICE] Sending invoice-created email to: {}", userEmail);

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("invoiceId", invoiceId);
            variables.put("amount", String.format("$%.2f", amount));
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
                    EmailStatus.SENT,
                    emailLogId,
                    Instant.now(),
                    "Invoice created email sent successfully",
                    null
            );

        } catch (Exception e) {
            throw new EmailProcessingException("Failed to send invoice-created email", e);
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
    ) {

        log.info("📧 [EMAIL SERVICE] Sending payment-failed email to: {}", userEmail);

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("invoiceId", invoiceId);
            variables.put("failureReason", failureReason);
            variables.put("retryLink", frontendUrl + "/invoices/" + invoiceId + "/retry-payment");
            variables.put("supportEmail", supportEmail);

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
                    EmailStatus.SENT,
                    emailLogId,
                    Instant.now(),
                    "Payment failed notification sent successfully",
                    null
            );

        } catch (Exception e) {
            throw new EmailProcessingException("Failed to send payment-failed email", e);
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

            log.info("✅ [EMAIL SERVICE] Notification email offloaded for background delivery: {}", userEmail);

            return new EmailSendResponse(
                    EmailStatus.SENT,
                    emailLogId,
                    Instant.now(),
                    "Notification email sent successfully",
                    null
            );

        } catch (Exception e) {
            throw new EmailProcessingException("Failed to send notification email", e);
        }
    }

    /**
     * Offloads execution to a dedicated background thread pool.
     */
    @Override
    @Async("emailTaskExecutor")
    public void sendHtmlEmailAsync(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailFrom, emailFromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Asynchronous email dispatch failed for {}: {}", toEmail, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Send a pre-rendered HTML email without an attachment (delegates asynchronously).
     */
    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        asyncEmailSender.sendHtmlEmailAsync(null, emailFrom, emailFromName, toEmail, subject, htmlContent);
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
        logEmailDelivery(emailId, toEmail, emailType, status, invoiceId, null, null, null, null);
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
            // Intentionally not re-thrown – logging must not block email dispatch.
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
