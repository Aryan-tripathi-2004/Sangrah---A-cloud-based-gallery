package com.example.Billing.application.service.impl;

import com.example.Billing.api.dto.response.CostEstimateDTO;
import com.example.Billing.api.dto.response.StorageUsageLedgerDTO;
import com.example.Billing.application.service.interfaces.ICostEstimationService;
import com.example.Billing.infrastructure.client.MediaServiceClient;
import com.example.Billing.infrastructure.mapper.StorageUsageMapper;
import com.example.Billing.infrastructure.persistence.repository.StorageUsageLedgerRepository;
import com.example.Billing.shared.util.BillingCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostEstimationServiceImpl implements ICostEstimationService {

    private final StorageUsageLedgerRepository ledgerRepository;
    private final MediaServiceClient mediaServiceClient;
    private final BillingCalculator calculator;
    private final StorageUsageMapper storageUsageMapper;

    @Override
    public CostEstimateDTO estimateCurrentMonthCost(String userId) {
        log.info("Estimating current month cost for user: {}", userId);

        Instant monthStart = calculator.getCurrentMonthStart();
        Instant now = Instant.now();

        List<StorageUsageLedgerDTO> currentMonthEntries = getLedgerEntriesForPeriod(userId, monthStart, now);

        BigDecimal currentImageGBDays = BigDecimal.ZERO;
        BigDecimal currentVideoGBDays = BigDecimal.ZERO;
        int daysElapsed = Math.max(1, (int) ChronoUnit.DAYS.between(monthStart, now));

        for (StorageUsageLedgerDTO entry : currentMonthEntries) {
            BigDecimal gbDays = calculator.calculateGBDays(
                    entry.sizeBytes(),
                    entry.startAt(),
                    entry.endAt()
            );
            currentImageGBDays = currentImageGBDays.add(gbDays);
        }

        BigDecimal totalGBDays = currentImageGBDays.add(currentVideoGBDays);
        BigDecimal imageCost = calculator.calculateCost(currentImageGBDays);
        BigDecimal videoCost = calculator.calculateCost(currentVideoGBDays);
        BigDecimal currentCost = imageCost.add(videoCost);
        BigDecimal projectedTotal = calculator.projectMonthlyTotal(totalGBDays, daysElapsed);

        CostEstimateDTO.CurrentMonthDataDTO monthData = CostEstimateDTO.CurrentMonthDataDTO.builder()
                .startDate(monthStart)
                .daysElapsed(daysElapsed)
                .estimatedGBDays(totalGBDays)
                .estimatedCost(currentCost)
                .projectedMonthlyTotal(projectedTotal)
                .build();

        CostEstimateDTO.StorageBreakdownDTO breakdown = CostEstimateDTO.StorageBreakdownDTO.builder()
                .images(CostEstimateDTO.StorageBreakdownDTO.StorageTypeDTO.builder()
                        .gbDays(currentImageGBDays)
                        .cost(imageCost)
                        .build())
                .videos(CostEstimateDTO.StorageBreakdownDTO.StorageTypeDTO.builder()
                        .gbDays(currentVideoGBDays)
                        .cost(videoCost)
                        .build())
                .build();

        BigDecimal costPerDay = currentCost.divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP);

        return CostEstimateDTO.builder()
                .currentMonthData(monthData)
                .storageBreakdown(breakdown)
                .costPerDay(costPerDay)
                .ratePerGBDay(calculator.getStorageRatePerGbDay())
                .build();
    }

    @Override
    public BillingCostDetails calculateCostForPeriod(String userId, Instant startDate, Instant endDate) {
        List<StorageUsageLedgerDTO> entries = getLedgerEntriesForPeriod(userId, startDate, endDate);

        BigDecimal totalImageGBDays = BigDecimal.ZERO;
        int fileCount = 0;

        for (StorageUsageLedgerDTO entry : entries) {
            Instant fileStart = entry.startAt() == null || entry.startAt().isBefore(startDate) ? startDate : entry.startAt();
            Instant fileEnd = entry.endAt() == null || entry.endAt().isAfter(endDate) ? endDate : entry.endAt();

            if (fileStart.isBefore(fileEnd)) {
                BigDecimal gbDays = calculator.calculateGBDays(entry.sizeBytes(), fileStart, fileEnd);
                totalImageGBDays = totalImageGBDays.add(gbDays);
                fileCount++;
            }
        }

        BigDecimal imageCost = calculator.calculateCost(totalImageGBDays);

        return BillingCostDetails.builder()
                .imageGBDays(totalImageGBDays)
                .imageCost(imageCost)
                .videoGBDays(BigDecimal.ZERO)
                .videoCost(BigDecimal.ZERO)
                .totalGBDays(totalImageGBDays)
                .totalCost(imageCost)
                .fileCount(fileCount)
                .build();
    }

    private List<StorageUsageLedgerDTO> getLedgerEntriesForPeriod(String userId, Instant start, Instant end) {
        try {
            List<StorageUsageLedgerDTO> remoteEntries = mediaServiceClient.getUserStorageLedger(userId, start, end);
            if (remoteEntries != null && !remoteEntries.isEmpty()) {
                return remoteEntries;
            }
        } catch (Exception e) {
            log.warn("Could not query MediaService storage ledger, falling back to local repository: {}", e.getMessage());
        }

        return ledgerRepository.findByUserIdAndCreatedAtGreaterThanEqual(userId, start).stream()
                .map(storageUsageMapper::toLedgerDTO)
                .toList();
    }
}
