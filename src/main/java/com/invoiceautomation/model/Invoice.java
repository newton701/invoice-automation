package com.invoiceautomation.model;

import com.invoiceautomation.enums.LifecycleStatus;
import com.invoiceautomation.enums.MatchStatus;
import com.invoiceautomation.enums.SourceChannel;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Header totals are "as printed" by the vendor, so they are NOT derived here:
 * the generator may deliberately make them wrong to create labelled defects.
 * matchStatus is null until the matcher has run.
 */
public record Invoice(
        String invoiceId, String invoiceNumber, String vendorId, String poId,
        LocalDate invoiceDate, LocalDate dueDate,
        BigDecimal subtotal, BigDecimal taxTotal, BigDecimal grandTotal,
        SourceChannel sourceChannel, LifecycleStatus lifecycleStatus, MatchStatus matchStatus) {

    public Invoice withLifecycleStatus(LifecycleStatus status) {
        return new Invoice(invoiceId, invoiceNumber, vendorId, poId, invoiceDate, dueDate,
                subtotal, taxTotal, grandTotal, sourceChannel, status, matchStatus);
    }

    public Invoice withMatchStatus(MatchStatus status) {
        return new Invoice(invoiceId, invoiceNumber, vendorId, poId, invoiceDate, dueDate,
                subtotal, taxTotal, grandTotal, sourceChannel, lifecycleStatus, status);
    }
}