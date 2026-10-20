package com.example.Billing.application.service;

import com.example.Billing.api.dto.response.InvoiceDTO;
import com.example.Billing.api.dto.response.InvoiceGenerationResultResponse;
import com.example.Billing.api.dto.response.StorageUsageLedgerDTO;
import com.example.Billing.application.service.interfaces.IBillingService;
import com.example.Billing.application.service.interfaces.IInvoiceGenerationService;
import com.example.Billing.infrastructure.event.InvoiceCreatedEvent;
import com.example.Billing.infrastructure.feign.GalleryServiceClient;
import com.example.Billing.shared.util.BillingCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashSet;
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
public class InvoiceGenerationService implements IInvoiceGenerationService {

    private static final int LEDGER_PAGE_SIZE = 500;

    private final GalleryServiceClient galleryServiceClient;
    private final IBillingService billingService;
    private final BillingCalculator billingCalculator;
    private final ApplicationEventPublisher eventPublisher;

    private final SequenceGeneratorService sequenceGeneratorService;

    /**
     * Main job: Generates monthly invoices for all users with storage usage
     * Scheduled to run on the 1st of each month at 00:00 UTC
     */
    @Scheduled(cron = "0 0 1 * * *")  // 1st of each month, 00:00 UTC
    public void generateMonthlyInvoices() {
        log.info("📊 ===== STARTING MONTHLY INVOICE GENERATION JOB =====");

        try {
            InvoiceGenerationResultResponse result = processMonthlyInvoiceGeneration();

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
    public InvoiceGenerationResultResponse processMonthlyInvoiceGeneration() {
        // Step 1: Get previous month boundaries
        Instant[] monthRange = billingCalculator.getPreviousMonthRange();
        Instant startOfPreviousMonth = monthRange[0];
        Instant endOfPreviousMonth = monthRange[1];

        log.info("📅 Generating invoices for period: {} to {}",
            startOfPreviousMonth, endOfPreviousMonth);

        // Step 2: Query Gallery service for unique active users via paginated API
        Set<String> userIds = fetchUserIdsWithStorageUsage(startOfPreviousMonth, endOfPreviousMonth);

        log.info("👥 Found {} unique users with storage usage", userIds.size());

        // Step 3: Generate invoices for each user
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

        return new InvoiceGenerationResultResponse(
            Instant.now(),
            generatedCount,
            failureCount,
            startOfPreviousMonth,
            endOfPreviousMonth
        );
    }

    /**
     * Helper method to iterate through ledger entries in bounded pages
     * to avoid OutOfMemoryError when dealing with massive datasets.
     */
    private Set<String> fetchUserIdsWithStorageUsage(Instant startDate, Instant endDate) {
        Set<String> userIds = new HashSet<>();
        int page = 0;
        long totalEntries = 0;
        Page<StorageUsageLedgerDTO> ledgerPage;

        do {
            ledgerPage = galleryServiceClient.getLedgerEntries(startDate, endDate, page, LEDGER_PAGE_SIZE);
            if (ledgerPage != null && ledgerPage.hasContent()) {
                for (StorageUsageLedgerDTO entry : ledgerPage.getContent()) {
                    if (entry.userId() != null && !entry.userId().isBlank()) {
                        userIds.add(entry.userId());
                    }
                }
                totalEntries += ledgerPage.getNumberOfElements();
            }
            page++;
        } while (ledgerPage != null && ledgerPage.hasNext());

        log.info("📋 Processed {} total ledger entries across {} page(s)", totalEntries, page);
        return userIds;
    }

    /**
     * Generate a single user's invoice for a billing period
     */
    private void generateInvoiceForUser(String userId, Instant startDate, Instant endDate) {
        log.info("📄 Generating invoice for user: {} from {} to {}",
            userId, startDate, endDate);

        // Generate unique invoice ID (format: INV-YYYY-MM-XXXXX)
        String invoiceId = billingCalculator.generateInvoiceId((int) sequenceGeneratorService.generateSequence("invoice_sequence"));

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
    public InvoiceGenerationResultResponse processCurrentMonthInvoiceGeneration() {
        log.info("📊 ===== GENERATING INVOICES FOR CURRENT MONTH (TEST) =====");

        // Get current month boundaries
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        YearMonth currentMonth = YearMonth.from(today);

        Instant startOfCurrentMonth = currentMonth.atDay(1)
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant();

        Instant endOfCurrentMonth = currentMonth.atEndOfMonth()
            .atStartOfDay(ZoneId.of("UTC"))
            .plusSeconds(86400)
            .toInstant();

        log.info("📅 Generating invoices for CURRENT month: {} to {}",
            startOfCurrentMonth, endOfCurrentMonth);

        // Step 2: Query Gallery service for unique active users via paginated API
        Set<String> userIds = fetchUserIdsWithStorageUsage(startOfCurrentMonth, endOfCurrentMonth);

        log.info("👥 Found {} unique users with storage usage in current month", userIds.size());

        // Generate invoices for each user
        int generatedCount = 0;
        int failureCount = 0;

        for (String userId : userIds) {
            try {
                generateInvoiceForUser(userId, startOfCurrentMonth, endOfCurrentMonth);
                generatedCount++;
                log.debug("✅ Invoice generated for user: {}", userId);

            } catch (Exception e) {
                log.error("❌ Failed to generate invoice for user: {}", userId, e);
                failureCount++;
            }
        }

        log.info("✅ ===== CURRENT MONTH INVOICE GENERATION COMPLETED =====");
        log.info("📈 Summary: Generated {} invoices, {} failures",
            generatedCount, failureCount);

        return new InvoiceGenerationResultResponse(
            Instant.now(),
            generatedCount,
            failureCount,
            startOfCurrentMonth,
            endOfCurrentMonth
        );
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
}
