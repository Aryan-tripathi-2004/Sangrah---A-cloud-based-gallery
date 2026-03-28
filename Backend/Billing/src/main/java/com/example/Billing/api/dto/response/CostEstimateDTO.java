package com.example.Billing.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostEstimateDTO {

    private CurrentMonthDataDTO currentMonthData;
    private StorageBreakdownDTO storageBreakdown;
    private Double costPerDay;
    private Double ratePerGBDay;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CurrentMonthDataDTO {
        private Instant startDate;
        private Integer daysElapsed;
        private Double estimatedGBDays;
        private Double estimatedCost;
        private Double projectedMonthlyTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageBreakdownDTO {
        private StorageTypeDTO images;
        private StorageTypeDTO videos;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class StorageTypeDTO {
            private Double gbDays;
            private Double cost;
        }
    }
}
