package com.example.Billing.api.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
public record CostEstimateDTO(
    CurrentMonthDataDTO currentMonthData,
    StorageBreakdownDTO storageBreakdown,
    BigDecimal costPerDay,
    BigDecimal ratePerGBDay
) {
    @Builder
    public record CurrentMonthDataDTO(
        Instant startDate,
        Integer daysElapsed,
        BigDecimal estimatedGBDays,
        BigDecimal estimatedCost,
        BigDecimal projectedMonthlyTotal
    ) {}

    @Builder
    public record StorageBreakdownDTO(
        StorageTypeDTO images,
        StorageTypeDTO videos
    ) {
        @Builder
        public record StorageTypeDTO(
            BigDecimal gbDays,
            BigDecimal cost
        ) {}
    }
}
