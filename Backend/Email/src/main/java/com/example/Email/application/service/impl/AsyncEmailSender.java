package com.example.Email.application.service.impl;

import com.example.Email.infrastructure.persistence.document.EmailLogDocument;
import com.example.Email.infrastructure.persistence.repository.EmailLogRepository;
import com.example.Email.shared.enums.EmailStatus;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Optional;

/**
 * Dedicated asynchronous email dispatcher component.
 * Offloads SMTP network I/O from Tomcat HTTP worker threads to the background thread pool.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncEmailSender {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;

    /**
     * Offloads execution to a dedicated background thread pool.
     */
    @Async("emailTaskExecutor")
    public void sendHtmlEmailAsync(String emailLogId, String fromEmail, String fromName, String toEmail, String subject, String htmlContent) {
        log.info("🚀 [ASYNC SENDER] Offloading HTML email dispatch for log {} to {} on thread: {}",
                emailLogId, toEmail, Thread.currentThread().getName());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ [ASYNC SENDER] Asynchronous email dispatch succeeded for log ID {}: to={}", emailLogId, toEmail);
            updateEmailStatus(emailLogId, EmailStatus.SENT, null);
        } catch (Exception e) {
            log.error("❌ [ASYNC SENDER] Asynchronous email dispatch failed for {}: {}", toEmail, e.getMessage(), e);
            updateEmailStatus(emailLogId, EmailStatus.FAILED, e.getMessage());
        }
    }

    /**
     * Offloads execution of an email with attachment to a dedicated background thread pool.
     */
    @Async("emailTaskExecutor")
    public void sendEmailWithAttachmentAsync(String emailLogId, String fromEmail, String fromName, String toEmail,
                                             String subject, String htmlContent, String attachmentFilename, byte[] attachmentBytes) {
        log.info("🚀 [ASYNC SENDER] Offloading email with attachment dispatch for log {} to {} on thread: {}",
                emailLogId, toEmail, Thread.currentThread().getName());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            if (attachmentBytes != null && attachmentBytes.length > 0) {
                helper.addAttachment(attachmentFilename, () -> new ByteArrayInputStream(attachmentBytes));
                log.info("📎 [ASYNC SENDER] Attachment added: {} ({} bytes)", attachmentFilename, attachmentBytes.length);
            }

            mailSender.send(message);
            log.info("✅ [ASYNC SENDER] Asynchronous email with attachment dispatch succeeded for log ID {}: to={}",
                    emailLogId, toEmail);
            updateEmailStatus(emailLogId, EmailStatus.SENT, null);
        } catch (Exception e) {
            log.error("❌ [ASYNC SENDER] Asynchronous email with attachment dispatch failed for {}: {}",
                    toEmail, e.getMessage(), e);
            updateEmailStatus(emailLogId, EmailStatus.FAILED, e.getMessage());
        }
    }

    private void updateEmailStatus(String emailLogId, EmailStatus status, String failureReason) {
        if (emailLogId == null) {
            return;
        }
        try {
            Optional<EmailLogDocument> logOpt = emailLogRepository.findByEmailId(emailLogId);
            if (logOpt.isPresent()) {
                EmailLogDocument emailLog = logOpt.get();
                emailLog.setStatus(status);
                if (status == EmailStatus.SENT) {
                    emailLog.setSentAt(Instant.now());
                    emailLog.setAttachmentBytes(null);
                } else if (status == EmailStatus.FAILED) {
                    emailLog.setFailureReason(failureReason);
                }
                emailLogRepository.save(emailLog);
                log.info("📝 [ASYNC SENDER] Updated email delivery log {} to status {}", emailLogId, status);
            }
        } catch (Exception ex) {
            log.error("❌ [ASYNC SENDER] Failed to update email log {}: {}", emailLogId, ex.getMessage(), ex);
        }
    }
}
