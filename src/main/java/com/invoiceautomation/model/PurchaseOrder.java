package com.invoiceautomation.model;

import com.invoiceautomation.enums.PoStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrder(
        String poId, String vendorId, LocalDate poDate, LocalDate expectedDeliveryDate,
        PoStatus status, String currency, BigDecimal totalAmount) {

    /** total_amount = sum of line amounts. */
    public static PurchaseOrder of(String poId, String vendorId, LocalDate poDate,
                                   LocalDate expectedDeliveryDate, PoStatus status,
                                   List<PurchaseOrderLine> lines) {
        BigDecimal total = lines.stream().map(PurchaseOrderLine::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PurchaseOrder(poId, vendorId, poDate, expectedDeliveryDate, status, "INR", total);
    }

    /** Status depends on receipts, which are generated after the PO. */
    public PurchaseOrder withStatus(PoStatus newStatus) {
        return new PurchaseOrder(poId, vendorId, poDate, expectedDeliveryDate, newStatus, currency, totalAmount);
    }
}