package com.example.Billing.application.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

import com.example.Billing.application.service.interfaces.ICostEstimationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostEstimationServiceImpl implements ICostEstimationService {

    private final StorageUsageLedgerRepository ledgerRepository;
    private final BillingCalculator calculator;

    /**
     * Estimate current month's cost based on usage so far
     * @param userId User ID
     * @return Cost estimate with projection
     */
    public CostEstimateDTO estimateCurrentMonthCost(String userId) {
        log.info("dY\"S Estimating current month cost for user: {}", userId);

        Instant monthStart = calculator.getCurrentMonthStart();
        Instant now = Instant.now();

        // Get all active ledger entries for current month
        List<StorageUsageLedgerDocument> currentMonthEntries =
            ledgerRepository.findByUserIdAndCreatedAtGreaterThanEqual(userId, monthStart);

        log.info("dY\"< Found {} storage entries for user {} in current month", currentMonthEntries.size(), userId);

        // Calculate current usage
        BigDecimal currentImageGBDays = BigDecimal.ZERO;
        BigDecimal currentVideoGBDays = BigDecimal.ZERO;
        int daysElapsed = (int) ChronoUnit.DAYS.between(monthStart, now);
        if (daysElapsed <= 0) daysElapsed = 1;

        for (StorageUsageLedgerDocument entry : currentMonthEntries) {
            BigDecimal gbDays = calculator.calculateGBDays(entry.getSizeBytes(), entry.getStartAt(), entry.getEndAt());

            // Determine type (would need mediaType field - for now assume all images)
            // TODO: Get media type from Gallery service or add to ledger
            currentImageGBDays = currentImageGBDays.add(gbDays);
        }

        BigDecimal currentCost = calculator.calculateCost(currentImageGBDays);
        BigDecimal projectedTotal = calculator.projectMonthlyTotal(currentImageGBDays, daysElapsed);

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
                .gbDays(BigDecimal.ZERO)
                .cost(BigDecimal.ZERO)
                .build();

        CostEstimateDTO.StorageBreakdownDTO breakdown = CostEstimateDTO.StorageBreakdownDTO.builder()
            .images(images)
            .videos(videos)
            .build();

        BigDecimal costPerDay = daysElapsed > 0 ? currentCost.divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        log.info("dY' Cost estimate: ${} current | ${} projected | ${}/day",
            currentCost, projectedTotal, costPerDay);

        return CostEstimateDTO.builder()
            .currentMonthData(monthData)
            .storageBreakdown(breakdown)
            .costPerDay(costPerDay)
            .ratePerGBDay(BigDecimal.valueOf(0.01))
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
        log.info("dY\"S Calculating cost for user {} from {} to {}", userId, startDate, endDate);

        List<StorageUsageLedgerDocument> entries =
            ledgerRepository.findByUserIdAndCreatedAtGreaterThanEqual(userId, startDate);

        BigDecimal totalImageGBDays = BigDecimal.ZERO;
        BigDecimal totalVideoGBDays = BigDecimal.ZERO;
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

            BigDecimal gbDays = calculator.calculateGBDays(entry.getSizeBytes(), periodStart, periodEnd);

            // TODO: Get media type from Gallery service
            totalImageGBDays = totalImageGBDays.add(gbDays);
            fileCount++;
        }

        BigDecimal imageCost = calculator.calculateCost(totalImageGBDays);
        BigDecimal totalCost = imageCost;  // + videoCost when implemented

        log.info("dY' Period cost calculated: ${} ({} files, {} GB-days)",
            totalCost, fileCount, totalImageGBDays);

        return BillingCostDetails.builder()
            .imageGBDays(totalImageGBDays)
            .imageCost(imageCost)
            .videoGBDays(BigDecimal.ZERO)
            .videoCost(BigDecimal.ZERO)
            .totalGBDays(totalImageGBDays)
            .totalCost(totalCost)
            .fileCount(fileCount)
            .build();
    }

}
