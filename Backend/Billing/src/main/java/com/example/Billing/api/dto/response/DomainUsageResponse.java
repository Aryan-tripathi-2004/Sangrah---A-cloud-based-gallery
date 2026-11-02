package com.example.Billing.api.dto.response;

/**
 * Response DTO for per-domain storage usage summary.
 * Replaces raw Map<String, Object> in BillingUsageController.
 */
public record DomainUsageResponse(
        String domain,
        long bytesSeconds
) {}
