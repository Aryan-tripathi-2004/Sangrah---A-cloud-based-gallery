package com.example.Billing.application.service.interfaces;

import com.example.Billing.api.dto.response.DomainUsageResponse;

/**
 * Service interface for per-domain billing usage summaries.
 * Stub implementation — real data will be wired in a future milestone.
 */
public interface IBillingUsageService {

    /**
     * Get storage usage summary for the Gallery domain
     */
    DomainUsageResponse getGalleryUsage();

    /**
     * Get storage usage summary for the Events domain
     */
    DomainUsageResponse getEventUsage();
}
