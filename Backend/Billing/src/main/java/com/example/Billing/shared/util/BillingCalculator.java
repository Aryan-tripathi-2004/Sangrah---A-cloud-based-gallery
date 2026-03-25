package com.example.Billing.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class BillingCalculator {

    public static final double STORAGE_RATE_PER_GB_DAY = 0.01;  // $0.01 per GB-day
    public static final double BYTES_PER_GB = 1_073_741_824.0;  // 1 GB

    /**
     * Calculate byte-days for a stored file
     * @param sizeBytes File size in bytes
     * @param uploadDate When file was uploaded
     * @param deletedDate When file was deleted (null if still stored)
     * @return GB-days
     */
    public Double calculateGBDays(Long sizeBytes, Instant uploadDate, Instant deletedDate) {
        if (sizeBytes == null || uploadDate == null) {
            return 0.0;
        }

        // Determine end date
        Instant endDate = deletedDate != null ? deletedDate : Instant.now();

        // Calculate days stored
        long daysBetween = ChronoUnit.DAYS.between(uploadDate, endDate);
        if (daysBetween <= 0) {
            daysBetween = 1;  // Minimum 1 day
        }

        // Convert bytes to GB and multiply by days
        double gbAmount = sizeBytes / BYTES_PER_GB;
        double gbDays = gbAmount * daysBetween;

        log.debug("📊 GB-Days calculation: {} bytes, {} days = {:.2f} GB-days",
            sizeBytes, daysBetween, gbDays);

        return gbDays;
    }

    /**
     * Calculate cost for GB-days at storage rate
     * @param gbDays GB-days to charge for
     * @return Cost in dollars
     */
    public Double calculateCost(Double gbDays) {
        if (gbDays == null || gbDays <= 0) {
            return 0.0;
        }
        return gbDays * STORAGE_RATE_PER_GB_DAY;
    }

    /**
     * Calculate cost with tax
     * @param subtotal Cost before tax
     * @param taxRate Tax rate as decimal (e.g., 0.08 for 8%)
     * @return Total with tax
     */
    public Double calculateWithTax(Double subtotal, Double taxRate) {
        if (subtotal == null || subtotal <= 0) {
            return 0.0;
        }
        if (taxRate == null || taxRate <= 0) {
            return subtotal;
        }
        double tax = subtotal * taxRate;
        return subtotal + tax;
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
            .plusSeconds(86400)  // Add 1 day
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
    public Double projectMonthlyTotal(Double currentGBDays, Integer daysElapsed) {
        if (currentGBDays == null || currentGBDays <= 0 || daysElapsed == null || daysElapsed <= 0) {
            return 0.0;
        }

        // Project to 31 days (conservative estimate)
        double projectedGBDays = (currentGBDays / daysElapsed) * 31;
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
    public String formatCurrency(Double amount) {
        if (amount == null) {
            return "$0.00";
        }
        return String.format("$%.2f", amount);
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
