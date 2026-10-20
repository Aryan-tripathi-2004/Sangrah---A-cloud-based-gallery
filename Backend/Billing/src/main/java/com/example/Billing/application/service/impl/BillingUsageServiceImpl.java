package com.example.Billing.application.service.impl;

import com.example.Billing.api.dto.response.DomainUsageResponse;
import com.example.Billing.application.service.interfaces.IBillingUsageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of IBillingUsageService.
 * Returns zero-valued responses until real domain usage queries are implemented.
 */
@Slf4j
@Service
public class BillingUsageServiceImpl implements IBillingUsageService {

    @Override
    public DomainUsageResponse getGalleryUsage() {
        log.debug("Fetching gallery domain usage (stub)");
        return new DomainUsageResponse("GALLERY", 0L);
    }

    @Override
    public DomainUsageResponse getEventUsage() {
        log.debug("Fetching events domain usage (stub)");
        return new DomainUsageResponse("EVENT", 0L);
    }
}
