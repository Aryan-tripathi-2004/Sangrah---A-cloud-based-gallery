package com.example.Email.application.service.interfaces;

/**
 * Service contract for reconciling orphaned or zombie email delivery logs.
 *
 * <p>Sweeps the database for emails stuck in {@code PENDING} state due to sudden
 * JVM termination, thread pool saturation, or container restarts, and requeues them
 * for background delivery.</p>
 */
public interface IEmailReconciliationService {

    /**
     * Sweep for emails stranded in PENDING status longer than the configured threshold
     * and re-dispatch them asynchronously.
     */
    void reconcilePendingEmails();
}
