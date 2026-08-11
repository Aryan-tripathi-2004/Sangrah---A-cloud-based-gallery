package com.example.Billing.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
public record CostEstimateDTO(
    CurrentMonthDataDTO currentMonthData,
    StorageBreakdownDTO storageBreakdown,
    Double costPerDay,
    Double ratePerGBDay
) {
    @Builder
    public record CurrentMonthDataDTO(
        Instant startDate,
        Integer daysElapsed,
        Double estimatedGBDays,
        Double estimatedCost,
        Double projectedMonthlyTotal
    ) {}

    @Builder
    public record StorageBreakdownDTO(
        StorageTypeDTO images,
        StorageTypeDTO videos
    ) {
        @Builder
        public record StorageTypeDTO(
            Double gbDays,
            Double cost
        ) {}
    }
}
