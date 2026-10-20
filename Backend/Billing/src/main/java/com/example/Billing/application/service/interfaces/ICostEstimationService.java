package com.example.Billing.application.service.interfaces;

import java.math.BigDecimal;

import com.example.Billing.api.dto.response.CostEstimateDTO;
import lombok.Builder;

import java.time.Instant;

public interface ICostEstimationService {
    CostEstimateDTO estimateCurrentMonthCost(String userId);
    BillingCostDetails calculateCostForPeriod(String userId, Instant startDate, Instant endDate);

    @Builder
    record BillingCostDetails(
        BigDecimal imageGBDays,
        BigDecimal imageCost,
        BigDecimal videoGBDays,
        BigDecimal videoCost,
        BigDecimal totalGBDays,
        BigDecimal totalCost,
        Integer fileCount
    ) {}
}
