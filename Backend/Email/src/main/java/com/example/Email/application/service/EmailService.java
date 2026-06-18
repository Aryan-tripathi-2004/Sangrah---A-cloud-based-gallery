package com.example.Email.application.service;

import com.example.Email.api.dto.EmailSendResponse;
import com.example.Email.infrastructure.persistence.document.EmailLogDocument;
import com.example.Email.infrastructure.persistence.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailLogRepository emailLogRepository;

    @Value("${app.email.from:noreply@sangrah.com}")
    private String emailFrom;

    @Value("${app.email.from.name:Sangrah Cloud Storage}")
    private String emailFromName;

    /**
     * Send Invoice Paid Email with PDF attachment
     */
    public EmailSendResponse sendInvoicePaidEmail(
            String invoiceId,
            String userId,
            String userEmail,
            Double amount,
            byte[] pdfContent
    ) {
        log.info("📧 ═══════════════════════════════════════════════════════════════");
        log.info("📧 [EMAIL SERVICE] Preparing to send invoice payment email");
        log.info("📧 [EMAIL SERVICE] Recipient Email: {}", userEmail);
        log.info("📧 [EMAIL SERVICE] Invoice ID: {}", invoiceId);
        log.info("📧 [EMAIL SERVICE] Amount: ${}", String.format("%.2f", amount));
        log.info("📧 [EMAIL SERVICE] PDF Size: {} bytes", pdfContent != null ? pdfContent.length : 0);
        log.info("📧 [EMAIL SERVICE] Sender: {} <{}>", emailFromName, emailFrom);
        log.info("📧 ═══════════════════════════════════════════════════════════════");

        try {
            // Prepare template variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("invoiceId", invoiceId);
            variables.put("amount", String.format("$%.2f", amount));
            variables.put("paidDate", Instant.now().toString());
            variables.put("downloadLink", "https://app.sangrah.com/invoices/" + invoiceId + "/download");

            log.info("📝 [EMAIL SERVICE] Rendering payment-success template...");
            // Render HTML template
            String htmlContent = renderTemplate("payment-success", variables);
            log.info("✅ [EMAIL SERVICE] Template rendered successfully");

            // Send email with PDF attachment
            log.info("💌 [EMAIL SERVICE] Creating MIME message...");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailFrom, emailFromName);
            helper.setTo(userEmail);
            helper.setSubject("Payment Confirmation - Invoice " + invoiceId);
            helper.setText(htmlContent, true);

            log.info("📧 [EMAIL SERVICE] Set to: {} | From: {} <{}>", userEmail, emailFromName, emailFrom);

            // Add PDF attachment if present
            if (pdfContent != null && pdfContent.length > 0) {
                helper.addAttachment("invoice_" + invoiceId + ".pdf", () -> new java.io.ByteArrayInputStream(pdfContent));
                log.info("📎 [EMAIL SERVICE] PDF attachment added: {} bytes", pdfContent.length);
            }

            log.info("🚀 [EMAIL SERVICE] Sending email via SMTP...");
            mailSender.send(message);
            log.info("✅ [EMAIL SERVICE] EMAIL SENT SUCCESSFULLY!");

            String emailLogId = UUID.randomUUID().toString();
            log.info("✅ [EMAIL SERVICE] Email ID: {}", emailLogId);

            // Log email delivery
            logEmailDelivery(emailLogId, userEmail, "invoice-paid", "sent", invoiceId);
            log.info("✅ [EMAIL SERVICE] Email logged in database");

            log.info("✅ [EMAIL SERVICE] ═══════════════════════════════════════════════════════════");
            log.info("✅ [EMAIL SERVICE] SENT TO: {}", userEmail);
            log.info("✅ [EMAIL SERVICE] ═══════════════════════════════════════════════════════════");

            return EmailSendResponse.builder()
                    .status("sent")
                    .emailId(emailLogId)
                    .timestamp(Instant.now())
                    .message("Payment confirmation email sent successfully to: " + userEmail)
                    .build();

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("❌ ═══════════════════════════════════════════════════════════════");
            log.error("❌ [EMAIL SERVICE] Failed to send invoice paid email to {}: {}", userEmail, e.getMessage());
            log.error("❌ [EMAIL SERVICE] Exception: ", e);
            log.error("❌ ═══════════════════════════════════════════════════════════════");

            String emailLogId = UUID.randomUUID().toString();
            logEmailDelivery(emailLogId, userEmail, "invoice-paid", "failed", invoiceId);

            return EmailSendResponse.builder()
                    .status("failed")
                    .emailId(emailLogId)
                    .timestamp(Instant.now())
                    .message("Failed to send payment confirmation email")
                    .errorReason(e.getMessage())
                    .build();
        }
    }

    /**
     * Send Invoice Created Email
     */
    public EmailSendResponse sendInvoiceCreatedEmail(
            String invoiceId,
            String userId,
            String userEmail,
            Double amount,
            String dueDate
    ) {
        log.info("📧 Sending invoice created email to: {}", userEmail);

        try {
            // Prepare template variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("invoiceId", invoiceId);
            variables.put("amount", String.format("$%.2f", amount));
            variables.put("dueDate", dueDate);
            variables.put("payLink", "https://app.sangrah.com/invoices/" + invoiceId + "/pay");

            // Render HTML template
            String htmlContent = renderTemplate("invoice-created", variables);

            // Send email
            sendHtmlEmail(userEmail, "Invoice " + invoiceId + " - Payment Due", htmlContent);

            String emailLogId = UUID.randomUUID().toString();
            log.info("✅ Invoice created email sent successfully: {}", emailLogId);

            // Log email delivery
            logEmailDelivery(emailLogId, userEmail, "invoice-created", "sent", invoiceId);

            return EmailSendResponse.builder()
                    .status("sent")
                    .emailId(emailLogId)
                    .timestamp(Instant.now())
                    .message("Invoice created email sent successfully")
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to send invoice created email to {}: {}", userEmail, e.getMessage(), e);

            String emailLogId = UUID.randomUUID().toString();
            logEmailDelivery(emailLogId, userEmail, "invoice-created", "failed", invoiceId);

            return EmailSendResponse.builder()
                    .status("failed")
                    .emailId(emailLogId)
                    .timestamp(Instant.now())
                    .message("Failed to send invoice email")
                    .errorReason(e.getMessage())
                    .build();
        }
    }

    /**
     * Send Payment Failed Email
     */
    public EmailSendResponse sendPaymentFailedEmail(
            String invoiceId,
            String userId,
            String userEmail,
            String failureReason
    ) {
        log.info("📧 Sending payment failed email to: {}", userEmail);

        try {
            // Prepare template variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("invoiceId", invoiceId);
            variables.put("failureReason", failureReason);
            variables.put("retryLink", "https://app.sangrah.com/invoices/" + invoiceId + "/retry-payment");
            variables.put("supportEmail", "support@sangrah.com");

            // Render HTML template
            String htmlContent = renderTemplate("payment-failed", variables);

            // Send email
            sendHtmlEmail(userEmail, "Payment Failed - Invoice " + invoiceId, htmlContent);

            String emailLogId = UUID.randomUUID().toString();
            log.info("✅ Payment failed email sent successfully: {}", emailLogId);

            // Log email delivery
            logEmailDelivery(emailLogId, userEmail, "payment-failed", "sent", invoiceId);

            return EmailSendResponse.builder()
                    .status("sent")
                    .emailId(emailLogId)
                    .timestamp(Instant.now())
                    .message("Payment failed notification sent successfully")
                    .build();

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("❌ Failed to send payment failed email to {}: {}", userEmail, e.getMessage(), e);

            String emailLogId = UUID.randomUUID().toString();
            logEmailDelivery(emailLogId, userEmail, "payment-failed", "failed", invoiceId);

            return EmailSendResponse.builder()
                    .status("failed")
                    .emailId(emailLogId)
                    .timestamp(Instant.now())
                    .message("Failed to send payment failed notification")
                    .errorReason(e.getMessage())
                    .build();
        }
    }

    /**
     * Generic method to send HTML email
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(emailFrom, emailFromName);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * Render Thymeleaf template with variables
     */
    private String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        return templateEngine.process(templateName, context);
    }

    /**
     * Log email delivery attempt to MongoDB
     */
    private void logEmailDelivery(String emailId, String toEmail, String emailType, String status, String invoiceId) {
        try {
            EmailLogDocument emailLog = EmailLogDocument.builder()
                    .emailId(emailId)
                    .toEmail(toEmail)
                    .fromEmail(emailFrom)
                    .emailType(emailType)
                    .invoiceId(invoiceId)
                    .status(status)
                    .sentAt(Instant.now())
                    .build();

            emailLogRepository.save(emailLog);
            log.info("📝 Email delivery logged: {}", emailId);
        } catch (Exception e) {
            log.error("❌ Failed to log email delivery: {}", e.getMessage(), e);
            // Don't rethrow - logging should not block email operations
        }
    }

    /**
     * Send generic notification email (for event notifications)
     */
    public EmailSendResponse sendNotificationEmail(
            String userEmail,
            String notificationType,
            String subject,
            String messageBody
    ) {
        log.info("📧 Sending notification email to: {}", userEmail);
        log.info("📧 Type: {}", notificationType);

        try {
            String emailLogId = UUID.randomUUID().toString();
            log.info("✅ Email ID: {}", emailLogId);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(emailFrom, emailFromName);
            helper.setTo(userEmail);
            helper.setSubject(subject);

            // Simple HTML body for notification
            String htmlBody = String.format(
                    "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #007bff;'>%s</h2>" +
                    "<p>%s</p>" +
                    "<p style='margin-top: 30px; font-size: 12px; color: #666;'>" +
                    "This is an automated notification from Sangrah Cloud Storage. Please do not reply to this email." +
                    "</p>" +
                    "</div>" +
                    "</body></html>",
                    subject,
                    messageBody
            );

            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
            log.info("✅ Notification email sent successfully to: {}", userEmail);

            // Log the email
            logEmailDelivery(emailLogId, userEmail, notificationType, "sent", null);

            return EmailSendResponse.builder()
                    .status("sent")
                    .emailId(emailLogId)
                    .timestamp(Instant.now())
                    .message("Notification email sent successfully")
                    .build();

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("❌ Failed to send notification email: {}", e.getMessage(), e);
            String emailLogId = UUID.randomUUID().toString();
            logEmailDelivery(emailLogId, userEmail, notificationType, "failed", null);

            return EmailSendResponse.builder()
                    .status("failed")
                    .emailId(emailLogId)
                    .timestamp(Instant.now())
                    .message("Failed to send notification email")
                    .errorReason(e.getMessage())
                    .build();
        }
    }
}

