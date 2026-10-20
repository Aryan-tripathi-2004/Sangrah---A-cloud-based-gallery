package com.example.Billing.api.dto.response;

import java.time.Instant;

/**
 * Response DTO for billing job execution status.
 * Replaces raw Map<String, Object> in BillingBatchAdminController.
 */
public record JobStatusResponse(
        String executionId,
        String status,
        Instant createdTime
) {}
