package com.invoiceautomation.util;

import com.invoiceautomation.enums.TaxCode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** All money arithmetic lives here so every class rounds the same way (2 dp, HALF_UP). */
public final class Money {

    private Money() { }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal lineAmount(int quantity, BigDecimal unitPrice) {
        return round(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    public static BigDecimal taxAmount(BigDecimal lineAmount, TaxCode taxCode) {
        return round(lineAmount.multiply(taxCode.rate()));
    }

    /** True when |a - b| <= tolerance. */
    public static boolean withinAbsolute(BigDecimal a, BigDecimal b, BigDecimal tolerance) {
        return a.subtract(b).abs().compareTo(tolerance) <= 0;
    }

    /** (actual - expected) / expected as a fraction, e.g. 0.0200 = +2%. Returns 0 if expected is 0. */
    public static BigDecimal variance(BigDecimal actual, BigDecimal expected) {
        if (expected.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return actual.subtract(expected).divide(expected, 4, RoundingMode.HALF_UP);
    }
}