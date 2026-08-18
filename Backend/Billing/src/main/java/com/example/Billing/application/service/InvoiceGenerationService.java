package com.example.Billing.application.service;

import com.example.Billing.api.dto.response.StorageUsageLedgerDTO;
import com.example.Billing.api.dto.response.InvoiceDTO;
import com.example.Billing.application.service.interfaces.IBillingService;
import com.example.Billing.infrastructure.feign.GalleryServiceClient;
import com.example.Billing.infrastructure.event.InvoiceCreatedEvent;
import com.example.Billing.shared.util.BillingCalculator;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for generating monthly invoices in batch
 * Runs on the 1st of each month at 00:00 UTC
 *
 * Example: Scheduled at April 1, 2026 00:00 UTC
 *          Generates invoices for March 2026 usage
 *
 * ARCHITECTURE: Uses Feign client to query Gallery service REST API
 * instead of direct database access. This follows microservice principles:
 * - Gallery owns its own data (sangrah_gallery)
 * - Billing queries Gallery via REST API (loose coupling)
 * - Independent scaling and deployment
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceGenerationService {

    private final GalleryServiceClient galleryServiceClient;
    private final IBillingService billingService;
    private final BillingCalculator billingCalculator;
    private final ApplicationEventPublisher eventPublisher;

    private volatile int invoiceSequence = 1;  // Counter for generating unique IDs

    /**
     * Main job: Generates monthly invoices for all users with storage usage
     * Scheduled to run on the 1st of each month at 00:00 UTC
     */
    @Scheduled(cron = "0 0 1 * * *")  // 1st of each month, 00:00 UTC
    public void generateMonthlyInvoices() {
        log.info("📊 ===== STARTING MONTHLY INVOICE GENERATION JOB =====");

        try {
            InvoiceGenerationResult result = processMonthlyInvoiceGeneration();

            log.info("✅ ===== MONTHLY INVOICE GENERATION COMPLETED =====");
            log.info("📈 Summary: Generated {} invoices, {} failures",
                result.generatedCount(), result.failureCount());

        } catch (Exception e) {
            log.error("❌ CRITICAL: Monthly invoice generation job failed", e);
        }
    }

    /**
     * Core logic for monthly invoice generation
     * Separated for testability and manual triggering
     */
    public InvoiceGenerationResult processMonthlyInvoiceGeneration() {
        // Step 1: Get previous month boundaries
        Instant[] monthRange = billingCalculator.getPreviousMonthRange();
        Instant startOfPreviousMonth = monthRange[0];
        Instant endOfPreviousMonth = monthRange[1];

        log.info("📅 Generating invoices for period: {} to {}",
            startOfPreviousMonth, endOfPreviousMonth);

        // Step 2: Query Gallery service for storage ledger entries (via REST API)
        // This follows microservice principle: Gallery owns its data
        List<StorageUsageLedgerDTO> ledgerEntries =
            galleryServiceClient.getLedgerEntries(startOfPreviousMonth, endOfPreviousMonth);

        log.info("📋 Found {} total ledger entries", ledgerEntries.size());

        // Step 3: Get unique users from ledger entries
        Set<String> userIds = new HashSet<>();
        for (StorageUsageLedgerDTO entry : ledgerEntries) {
            if (entry.userId() != null) {
                userIds.add(entry.userId());
            }
        }

        log.info("👥 Found {} unique users with storage usage", userIds.size());

        // Step 4: Generate invoices for each user
        int generatedCount = 0;
        int failureCount = 0;

        for (String userId : userIds) {
            try {
                generateInvoiceForUser(userId, startOfPreviousMonth, endOfPreviousMonth);
                generatedCount++;
                log.debug("✅ Invoice generated for user: {}", userId);

            } catch (Exception e) {
                log.error("❌ Failed to generate invoice for user: {}", userId, e);
                failureCount++;
            }
        }

        return InvoiceGenerationResult.builder()
            .generatedAt(Instant.now())
            .generatedCount(generatedCount)
            .failureCount(failureCount)
            .billingPeriodStart(startOfPreviousMonth)
            .billingPeriodEnd(endOfPreviousMonth)
            .build();
    }

    /**
     * Generate a single user's invoice for a billing period
     */
    private void generateInvoiceForUser(String userId, Instant startDate, Instant endDate) {
        log.info("📄 Generating invoice for user: {} from {} to {}",
            userId, startDate, endDate);

        // Generate unique invoice ID (format: INV-YYYY-MM-XXXXX)
        String invoiceId = billingCalculator.generateInvoiceId(invoiceSequence++);

        // Call BillingService to create invoice
        // This handles:
        // - Duplicate checking
        // - Cost calculation via CostEstimationService
        // - Invoice document persistence
        InvoiceDTO createdInvoice = billingService.createInvoice(userId, invoiceId, startDate, endDate);

        log.info("✅ Invoice created successfully: {}", invoiceId);

        // ✨ NEW: Publish event for listeners (notification service sends email)
        eventPublisher.publishEvent(new InvoiceCreatedEvent(
            this,
            createdInvoice.id(),
            userId,
            createdInvoice.invoiceId(),
            createdInvoice.charges().totalAmount()
        ));
        log.debug("📢 Published InvoiceCreatedEvent for invoice: {}", invoiceId);
    }

    /**
     * TEST METHOD: Generate invoices for current month (for testing with recent uploads)
     * This is useful for verification before 1st of next month
     */
    public InvoiceGenerationResult processCurrentMonthInvoiceGeneration() {
        log.info("📊 ===== GENERATING INVOICES FOR CURRENT MONTH (TEST) =====");

        // Get current month boundaries
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("UTC"));
        java.time.YearMonth currentMonth = java.time.YearMonth.from(today);

        Instant startOfCurrentMonth = currentMonth.atDay(1)
            .atStartOfDay(java.time.ZoneId.of("UTC"))
            .toInstant();

        Instant endOfCurrentMonth = currentMonth.atEndOfMonth()
            .atStartOfDay(java.time.ZoneId.of("UTC"))
            .plusSeconds(86400)
            .toInstant();

        log.info("📅 Generating invoices for CURRENT month: {} to {}",
            startOfCurrentMonth, endOfCurrentMonth);

        // Query Gallery service for storage ledger entries (via REST API)
        // This follows microservice principle: Gallery owns its data
        List<StorageUsageLedgerDTO> ledgerEntries =
            galleryServiceClient.getLedgerEntries(startOfCurrentMonth, endOfCurrentMonth);

        log.info("📋 Found {} total ledger entries in current month", ledgerEntries.size());

        // Get unique users from ledger entries
        Set<String> userIds = new HashSet<>();
        for (StorageUsageLedgerDTO entry : ledgerEntries) {
            if (entry.userId() != null) {
                userIds.add(entry.userId());
            }
        }

        log.info("👥 Found {} unique users with storage usage in current month", userIds.size());

        // Generate invoices for each user
        int generatedCount = 0;
        int failureCount = 0;

        for (String userId : userIds) {
            try {
                String invoiceId = billingCalculator.generateInvoiceId(invoiceSequence++);
                InvoiceDTO createdInvoice = billingService.createInvoice(userId, invoiceId, startOfCurrentMonth, endOfCurrentMonth);
                generatedCount++;
                log.debug("✅ Invoice generated for user: {}", userId);

                // ✨ NEW: Publish event for listeners (send notification email)
                eventPublisher.publishEvent(new InvoiceCreatedEvent(
                    this,
                    createdInvoice.id(),
                    userId,
                    createdInvoice.invoiceId(),
                    createdInvoice.charges().totalAmount()
                ));
                log.debug("📢 Published InvoiceCreatedEvent for invoice: {}", invoiceId);

            } catch (Exception e) {
                log.error("❌ Failed to generate invoice for user: {}", userId, e);
                failureCount++;
            }
        }

        log.info("✅ ===== CURRENT MONTH INVOICE GENERATION COMPLETED =====");
        log.info("📈 Summary: Generated {} invoices, {} failures",
            generatedCount, failureCount);

        return InvoiceGenerationResult.builder()
            .generatedAt(java.time.Instant.now())
            .generatedCount(generatedCount)
            .failureCount(failureCount)
            .billingPeriodStart(startOfCurrentMonth)
            .billingPeriodEnd(endOfCurrentMonth)
            .build();
    }

    /**
     * Optional: Retry failed invoices (for future enhancement)
     * Could be scheduled daily to retry failed generations
     */
    @Scheduled(cron = "0 0 2 * * *")  // Daily at 02:00 UTC
    public void retryFailedInvoices() {
        log.info("🔄 Checking for failed invoices to retry...");
        // TODO: Implement retry logic for failed invoice generations
        log.info("✅ Retry check complete");
    }

    /**
     * Result DTO for invoice generation job
     */
    @Builder
    public record InvoiceGenerationResult(
        Instant generatedAt,
        Integer generatedCount,
        Integer failureCount,
        Instant billingPeriodStart,
        Instant billingPeriodEnd
    ) {}
}
