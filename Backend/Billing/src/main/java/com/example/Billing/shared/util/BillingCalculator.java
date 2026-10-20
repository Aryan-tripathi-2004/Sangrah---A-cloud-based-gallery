package com.example.Billing.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class BillingCalculator {

    @Value("${billing.storage-rate-per-gb-day}")
    private BigDecimal storageRatePerGbDay;

    public static final double BYTES_PER_GB = 1_073_741_824.0;  // 1 GB

    /**
     * Calculate byte-days for a stored file
     * @param sizeBytes File size in bytes
     * @param uploadDate When file was uploaded
     * @param deletedDate When file was deleted (null if still stored)
     * @return GB-days
     */
    public BigDecimal calculateGBDays(Long sizeBytes, Instant uploadDate, Instant deletedDate) {
        if (sizeBytes == null || uploadDate == null) {
            return BigDecimal.ZERO;
        }

        // Determine end date
        Instant endDate = deletedDate != null ? deletedDate : Instant.now();

        // Calculate days stored
        long daysBetween = ChronoUnit.DAYS.between(uploadDate, endDate);
        if (daysBetween <= 0) {
            daysBetween = 1;  // Minimum 1 day
        }

        // Convert bytes to GB and multiply by days
        BigDecimal gbAmount = BigDecimal.valueOf(sizeBytes).divide(BigDecimal.valueOf(BYTES_PER_GB), 6, RoundingMode.HALF_UP);
        BigDecimal gbDays = gbAmount.multiply(BigDecimal.valueOf(daysBetween));

        log.debug("GB-Days calculation: {} bytes, {} days = {} GB-days",
            sizeBytes, daysBetween, gbDays);

        return gbDays;
    }

    /**
     * Calculate cost for GB-days at storage rate
     * @param gbDays GB-days to charge for
     * @return Cost in dollars
     */
    public BigDecimal calculateCost(BigDecimal gbDays) {
        if (gbDays == null || gbDays.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return gbDays.multiply(storageRatePerGbDay).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate cost with tax
     * @param subtotal Cost before tax
     * @param taxRate Tax rate as decimal (e.g., 0.08 for 8%)
     * @return Total with tax
     */
    public BigDecimal calculateWithTax(BigDecimal subtotal, BigDecimal taxRate) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (taxRate == null || taxRate.compareTo(BigDecimal.ZERO) <= 0) {
            return subtotal;
        }
        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        return subtotal.add(tax);
    }

    /**
     * Get the first and last day of previous month
     * @return [startOfPreviousMonth, endOfPreviousMonth]
     */
    public Instant[] getPreviousMonthRange() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        YearMonth previousMonth = YearMonth.from(today).minusMonths(1);

        Instant start = previousMonth.atDay(1)
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant();

        Instant end = previousMonth.atEndOfMonth()
            .atStartOfDay(ZoneId.of("UTC"))
            .plusDays(1) // Utilize declarative date arithmetic
            .toInstant();

        return new Instant[] { start, end };
    }

    /**
     * Get current month's start date
     */
    public Instant getCurrentMonthStart() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        return today.withDayOfMonth(1)
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant();
    }

    /**
     * Project monthly cost based on current usage
     * @param currentGBDays Current month's GB-days used so far
     * @param daysElapsed Days elapsed in current month
     * @return Projected total for full month
     */
    public BigDecimal projectMonthlyTotal(BigDecimal currentGBDays, Integer daysElapsed) {
        if (currentGBDays == null || currentGBDays.compareTo(BigDecimal.ZERO) <= 0 || daysElapsed == null || daysElapsed <= 0) {
            return BigDecimal.ZERO;
        }

        // Dynamically calculate the actual number of days in the current month
        int daysInCurrentMonth = YearMonth.now(ZoneId.of("UTC")).lengthOfMonth();

        BigDecimal projectedGBDays = currentGBDays
            .divide(BigDecimal.valueOf(daysElapsed), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(daysInCurrentMonth));

        return calculateCost(projectedGBDays);
    }

    /**
     * Format bytes to human-readable string
     */
    public String formatBytes(Long bytes) {
        if (bytes == null || bytes <= 0) return "0 B";

        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int unitIndex = 0;
        double size = bytes.doubleValue();

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    /**
     * Format amount as currency
     */
    public String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return String.format("$%.2f", amount.doubleValue());
    }

    /**
     * Generate invoice ID for month
     * Format: INV-YYYY-MM-XXXXX
     */
    public String generateInvoiceId(int sequenceNumber) {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        YearMonth previousMonth = YearMonth.from(today).minusMonths(1);

        return String.format("INV-%d-%02d-%05d",
            previousMonth.getYear(),
            previousMonth.getMonthValue(),
            sequenceNumber);
    }
}
