package com.invoiceautomation.config;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Every tunable number of the generator. Nothing else in the project hardcodes these.
 * Scenario percentages are of total invoices and must add up to 100.
 */
public record GeneratorConfig(
        long seed,
        int vendors, int purchaseOrders, int poLines, int receipts, int invoices,
        int cleanPct, int priceVariancePct, int quantityMismatchPct,
        int duplicatePct, int missingReceiptPct, int taxVendorErrorPct,
        BigDecimal priceTolerance, BigDecimal taxTolerance, BigDecimal headerTolerance,
        LocalDate windowStart, LocalDate windowEnd,
        String outputDir) {

    public GeneratorConfig {
        int total = cleanPct + priceVariancePct + quantityMismatchPct
                + duplicatePct + missingReceiptPct + taxVendorErrorPct;
        if (total != 100) {
            throw new IllegalArgumentException("Scenario percentages must sum to 100, got " + total);
        }
    }

    /** Values taken from the brief and docs/data_dictionary.md. */
    public static GeneratorConfig defaults() {
        return new GeneratorConfig(
                20261022L,
                300, 1500, 2200, 1800, 2000,
                70, 10, 8, 5, 4, 3,
                new BigDecimal("0.02"),   // price +/- 2%
                new BigDecimal("1.00"),   // tax tolerance in INR
                new BigDecimal("0.01"),   // header vs line sum
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 30),
                "data");
    }

    public GeneratorConfig withSeed(long newSeed) {
        return new GeneratorConfig(newSeed, vendors, purchaseOrders, poLines, receipts, invoices,
                cleanPct, priceVariancePct, quantityMismatchPct, duplicatePct, missingReceiptPct,
                taxVendorErrorPct, priceTolerance, taxTolerance, headerTolerance,
                windowStart, windowEnd, outputDir);
    }
}