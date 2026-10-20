package com.example.Email.application.service.impl;

import com.example.Email.application.service.interfaces.IEmailReconciliationService;
import com.example.Email.infrastructure.persistence.document.EmailLogDocument;
import com.example.Email.infrastructure.persistence.repository.EmailLogRepository;
import com.example.Email.shared.enums.EmailStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Scheduled reconciliation worker that recovers emails marooned in {@link EmailStatus#PENDING}.
 *
 * <p>Eliminates the "Zombie State via Unhandled Async Failures" anti-pattern. If a JVM crashes,
 * deployment occurs, or thread pool queues reject tasks after a database commit, this reconciler
 * detects the stranded records, increments retry counters, and resubmits them for delivery.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.email.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
public class EmailReconciliationServiceImpl implements IEmailReconciliationService {

    private final EmailLogRepository emailLogRepository;
    private final AsyncEmailSender asyncEmailSender;

    @Value("${app.email.from}")
    private String defaultFromEmail;

    @Value("${app.email.from.name}")
    private String defaultFromName;

    @Value("${app.email.reconciliation.threshold-minutes}")
    private long pendingThresholdMinutes;

    @Value("${app.email.reconciliation.max-retries}")
    private int maxRetries;

    @Override
    @Scheduled(cron = "${app.email.reconciliation.cron:0 */5 * * * *}")
    public void reconcilePendingEmails() {
        Instant cutoffTime = Instant.now().minus(Duration.ofMinutes(pendingThresholdMinutes));
        log.debug("🔍 [RECONCILIATION] Scanning for emails stuck in PENDING before {}", cutoffTime);

        List<EmailLogDocument> stuckEmails;
        try {
            stuckEmails = emailLogRepository.findByStatusAndCreatedAtBefore(EmailStatus.PENDING, cutoffTime);
        } catch (Exception ex) {
            log.error("❌ [RECONCILIATION] Failed to query pending email logs: {}", ex.getMessage(), ex);
            return;
        }

        if (stuckEmails.isEmpty()) {
            log.debug("✅ [RECONCILIATION] No zombie email records found.");
            return;
        }

        log.warn("⚠️ [RECONCILIATION] Discovered {} zombie email(s) stuck in PENDING. Initiating reconciliation sweep...",
                stuckEmails.size());

        for (EmailLogDocument emailLog : stuckEmails) {
            reconcileSingleEmail(emailLog);
        }
    }

    private void reconcileSingleEmail(EmailLogDocument emailLog) {
        String emailId = emailLog.getEmailId();
        int currentRetries = emailLog.getRetryCount() != null ? emailLog.getRetryCount() : 0;

        if (currentRetries >= maxRetries) {
            log.warn("❌ [RECONCILIATION] Email {} exceeded max retries ({} >= {}). Marking as FAILED.",
                    emailId, currentRetries, maxRetries);
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setFailureReason("Exceeded maximum reconciliation retry attempts (" + maxRetries + ")");
            emailLog.setAttachmentBytes(null);
            emailLogRepository.save(emailLog);
            return;
        }

        if (emailLog.getHtmlContent() == null || emailLog.getHtmlContent().isBlank()) {
            log.warn("⚠️ [RECONCILIATION] Email {} has no cached HTML content (legacy log). Marking as FAILED.", emailId);
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setFailureReason("Cannot reconcile email without stored content payload");
            emailLogRepository.save(emailLog);
            return;
        }

        int nextRetryCount = currentRetries + 1;
        emailLog.setRetryCount(nextRetryCount);
        emailLog.setLastRetryAt(Instant.now());
        emailLogRepository.save(emailLog);

        String fromEmail = emailLog.getFromEmail() != null ? emailLog.getFromEmail() : defaultFromEmail;
        String subject = emailLog.getSubject() != null ? emailLog.getSubject() : "Notification from Sangrah";

        try {
            if (emailLog.getAttachmentBytes() != null && emailLog.getAttachmentBytes().length > 0) {
                String filename = emailLog.getAttachmentFilename() != null ? emailLog.getAttachmentFilename() : "attachment.pdf";
                log.info("🚀 [RECONCILIATION] Requeuing email with attachment {} to {} (attempt {}/{})",
                        emailId, emailLog.getToEmail(), nextRetryCount, maxRetries);
                asyncEmailSender.sendEmailWithAttachmentAsync(
                        emailId,
                        fromEmail,
                        defaultFromName,
                        emailLog.getToEmail(),
                        subject,
                        emailLog.getHtmlContent(),
                        filename,
                        emailLog.getAttachmentBytes()
                );
            } else {
                log.info("🚀 [RECONCILIATION] Requeuing HTML email {} to {} (attempt {}/{})",
                        emailId, emailLog.getToEmail(), nextRetryCount, maxRetries);
                asyncEmailSender.sendHtmlEmailAsync(
                        emailId,
                        fromEmail,
                        defaultFromName,
                        emailLog.getToEmail(),
                        subject,
                        emailLog.getHtmlContent()
                );
            }
        } catch (Exception ex) {
            log.error("❌ [RECONCILIATION] Failed to requeue email {}: {}", emailId, ex.getMessage(), ex);
        }
    }
}
