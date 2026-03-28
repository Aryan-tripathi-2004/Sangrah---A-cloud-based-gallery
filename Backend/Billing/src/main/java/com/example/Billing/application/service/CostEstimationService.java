package com.example.Billing.application.service;

import com.example.Billing.api.dto.response.CostEstimateDTO;
import com.example.Billing.infrastructure.persistence.document.StorageUsageLedgerDocument;
import com.example.Billing.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import com.example.Billing.shared.util.BillingCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostEstimationService {

    private final StorageUsageLedgerRepository ledgerRepository;
    private final BillingCalculator calculator;

    /**
     * Estimate current month's cost based on usage so far
     * @param userId User ID
     * @return Cost estimate with projection
     */
    public CostEstimateDTO estimateCurrentMonthCost(String userId) {
        log.info("📊 Estimating current month cost for user: {}", userId);

        Instant monthStart = calculator.getCurrentMonthStart();
        Instant now = Instant.now();

        // Get all active ledger entries for current month
        List<StorageUsageLedgerDocument> currentMonthEntries =
            ledgerRepository.findByUserIdAndCreatedAtGreaterThanEqual(userId, monthStart);

        log.info("📋 Found {} storage entries for user {} in current month", currentMonthEntries.size(), userId);

        // Calculate current usage
        double currentImageGBDays = 0.0;
        double currentVideoGBDays = 0.0;
        int daysElapsed = (int) ChronoUnit.DAYS.between(monthStart, now);
        if (daysElapsed <= 0) daysElapsed = 1;

        for (StorageUsageLedgerDocument entry : currentMonthEntries) {
            double gbDays = calculator.calculateGBDays(entry.getSizeBytes(), entry.getStartAt(), entry.getEndAt());

            // Determine type (would need mediaType field - for now assume all images)
            // TODO: Get media type from Gallery service or add to ledger
            currentImageGBDays += gbDays;
        }

        double currentCost = calculator.calculateCost(currentImageGBDays);
        double projectedTotal = calculator.projectMonthlyTotal(currentImageGBDays, daysElapsed);

        // Build response
        CostEstimateDTO.CurrentMonthDataDTO monthData = CostEstimateDTO.CurrentMonthDataDTO.builder()
            .startDate(monthStart)
            .daysElapsed(daysElapsed)
            .estimatedGBDays(currentImageGBDays)
            .estimatedCost(currentCost)
            .projectedMonthlyTotal(projectedTotal)
            .build();

        CostEstimateDTO.StorageBreakdownDTO.StorageTypeDTO images =
            CostEstimateDTO.StorageBreakdownDTO.StorageTypeDTO.builder()
                .gbDays(currentImageGBDays)
                .cost(currentCost)
                .build();

        CostEstimateDTO.StorageBreakdownDTO.StorageTypeDTO videos =
            CostEstimateDTO.StorageBreakdownDTO.StorageTypeDTO.builder()
                .gbDays(0.0)
                .cost(0.0)
                .build();

        CostEstimateDTO.StorageBreakdownDTO breakdown = CostEstimateDTO.StorageBreakdownDTO.builder()
            .images(images)
            .videos(videos)
            .build();

        double costPerDay = daysElapsed > 0 ? currentCost / daysElapsed : 0.0;

        log.info("💰 Cost estimate: ${:.2f} current | ${:.2f} projected | ${:.2f}/day",
            currentCost, projectedTotal, costPerDay);

        return CostEstimateDTO.builder()
            .currentMonthData(monthData)
            .storageBreakdown(breakdown)
            .costPerDay(costPerDay)
            .ratePerGBDay(BillingCalculator.STORAGE_RATE_PER_GB_DAY)
            .build();
    }

    /**
     * Calculate actual cost for a specific date range
     * @param userId User ID
     * @param startDate Start of period
     * @param endDate End of period
     * @return Cost breakdown
     */
    public BillingCostDetails calculateCostForPeriod(String userId, Instant startDate, Instant endDate) {
        log.info("📊 Calculating cost for user {} from {} to {}", userId, startDate, endDate);

        List<StorageUsageLedgerDocument> entries =
            ledgerRepository.findByUserIdAndCreatedAtGreaterThanEqual(userId, startDate);

        double totalImageGBDays = 0.0;
        double totalVideoGBDays = 0.0;
        int fileCount = 0;

        for (StorageUsageLedgerDocument entry : entries) {
            // Only count files that existed during this period
            Instant fileStart = entry.getStartAt();
            Instant fileEnd = entry.getEndAt() != null ? entry.getEndAt() : endDate;

            // Skip if file doesn't overlap with billing period
            if (fileEnd.isBefore(startDate) || fileStart.isAfter(endDate)) {
                continue;
            }

            // Calculate days in billing period only
            Instant periodStart = fileStart.isBefore(startDate) ? startDate : fileStart;
            Instant periodEnd = fileEnd.isBefore(endDate) ? fileEnd : endDate;

            double gbDays = calculator.calculateGBDays(entry.getSizeBytes(), periodStart, periodEnd);

            // TODO: Get media type from Gallery service
            totalImageGBDays += gbDays;
            fileCount++;
        }

        double imageCost = calculator.calculateCost(totalImageGBDays);
        double totalCost = imageCost;  // + videoCost when implemented

        log.info("💰 Period cost calculated: ${:.2f} ({} files, {:.2f} GB-days)",
            totalCost, fileCount, totalImageGBDays);

        return BillingCostDetails.builder()
            .imageGBDays(totalImageGBDays)
            .imageCost(imageCost)
            .videoGBDays(0.0)
            .videoCost(0.0)
            .totalGBDays(totalImageGBDays)
            .totalCost(totalCost)
            .fileCount(fileCount)
            .build();
    }

    // Inner class for cost details
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class BillingCostDetails {
        private Double imageGBDays;
        private Double imageCost;
        private Double videoGBDays;
        private Double videoCost;
        private Double totalGBDays;
        private Double totalCost;
        private Integer fileCount;
    }
}
