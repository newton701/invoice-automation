
package com.invoiceautomation.model;

import com.invoiceautomation.enums.TaxCode;
import com.invoiceautomation.util.IdFactory;
import com.invoiceautomation.util.Money;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents one line on a vendor invoice.
 *
 * An invoice can contain multiple invoice lines.
 * poLineId is null when the invoice has no purchase order
 * or when the line cannot be linked to a PO line.
 *
 * Line amount, tax amount and line total are calculated
 * from the source line values.
 */
public record InvoiceLine(
        String invoiceLineId,
        String invoiceId,
        int lineNumber,
        String poLineId,
        String sku,
        int quantity,
        BigDecimal unitPrice,
        TaxCode taxCode,
        BigDecimal lineAmount,
        BigDecimal taxAmount,
        BigDecimal lineTotal) {

    public InvoiceLine {
        Objects.requireNonNull(invoiceLineId, "invoiceLineId cannot be null");
        Objects.requireNonNull(invoiceId, "invoiceId cannot be null");
        Objects.requireNonNull(sku, "sku cannot be null");
        Objects.requireNonNull(unitPrice, "unitPrice cannot be null");
        Objects.requireNonNull(taxCode, "taxCode cannot be null");

        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be positive");
        }

        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be positive");
        }

        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice cannot be negative");
        }
    }

    /**
     * Creates an invoice line linked to a purchase-order line.
     */
    public static InvoiceLine of(
            String invoiceId,
            int lineNumber,
            String poLineId,
            String sku,
            int quantity,
            BigDecimal unitPrice,
            TaxCode taxCode) {

        return create(
                invoiceId,
                lineNumber,
                poLineId,
                sku,
                quantity,
                unitPrice,
                taxCode
        );
    }

    /**
     * Creates an invoice line for an invoice without a purchase order.
     */
    public static InvoiceLine withoutPurchaseOrder(
            String invoiceId,
            int lineNumber,
            String sku,
            int quantity,
            BigDecimal unitPrice,
            TaxCode taxCode) {

        return create(
                invoiceId,
                lineNumber,
                null,
                sku,
                quantity,
                unitPrice,
                taxCode
        );
    }

    private static InvoiceLine create(
            String invoiceId,
            int lineNumber,
            String poLineId,
            String sku,
            int quantity,
            BigDecimal unitPrice,
            TaxCode taxCode) {

        Objects.requireNonNull(invoiceId, "invoiceId cannot be null");
        Objects.requireNonNull(unitPrice, "unitPrice cannot be null");
        Objects.requireNonNull(taxCode, "taxCode cannot be null");

        BigDecimal amount = Money.lineAmount(quantity, unitPrice);
        BigDecimal tax = Money.taxAmount(amount, taxCode);
        BigDecimal total = amount.add(tax);

        return new InvoiceLine(
                IdFactory.invoiceLineId(invoiceId, lineNumber),
                invoiceId,
                lineNumber,
                poLineId,
                sku,
                quantity,
                unitPrice,
                taxCode,
                amount,
                tax,
                total
        );
    }
}
