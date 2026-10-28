package com.invoiceautomation.model;

import com.invoiceautomation.enums.TaxCode;
import com.invoiceautomation.util.IdFactory;
import com.invoiceautomation.util.Money;

import java.math.BigDecimal;

public record PurchaseOrderLine(
        String poLineId, String poId, int lineNumber, String sku, String description,
        int orderedQty, BigDecimal unitPrice, TaxCode taxCode, BigDecimal lineAmount) {

    /** Derived field lineAmount is computed here, never passed in. */
    public static PurchaseOrderLine of(String poId, int lineNumber, String sku, String description,
                                       int orderedQty, BigDecimal unitPrice, TaxCode taxCode) {
        return new PurchaseOrderLine(IdFactory.poLineId(poId, lineNumber), poId, lineNumber, sku,
                description, orderedQty, unitPrice, taxCode, Money.lineAmount(orderedQty, unitPrice));
    }
}