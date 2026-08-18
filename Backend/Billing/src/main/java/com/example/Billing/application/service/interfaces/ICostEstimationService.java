package com.example.Billing.application.service.interfaces;

import com.example.Billing.api.dto.response.CostEstimateDTO;
import lombok.Builder;

import java.time.Instant;

public interface ICostEstimationService {
    CostEstimateDTO estimateCurrentMonthCost(String userId);
    BillingCostDetails calculateCostForPeriod(String userId, Instant startDate, Instant endDate);

    @Builder
    record BillingCostDetails(
        Double imageGBDays,
        Double imageCost,
        Double videoGBDays,
        Double videoCost,
        Double totalGBDays,
        Double totalCost,
        Integer fileCount
    ) {}
}
