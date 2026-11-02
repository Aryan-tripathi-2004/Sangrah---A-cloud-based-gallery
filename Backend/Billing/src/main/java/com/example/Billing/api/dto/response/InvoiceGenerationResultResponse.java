package com.example.Billing.api.dto.response;

import java.time.Instant;

/**
 * Response DTO for invoice generation job results.
 * Replaces InvoiceGenerationService.InvoiceGenerationResult inner class
 * being exposed directly as the API contract in BillingBatchAdminController.
 */
public record InvoiceGenerationResultResponse(
        Instant generatedAt,
        Integer generatedCount,
        Integer failureCount,
        Instant billingPeriodStart,
        Instant billingPeriodEnd
) {}
