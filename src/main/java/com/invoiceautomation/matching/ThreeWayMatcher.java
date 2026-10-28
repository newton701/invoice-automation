
package com.invoiceautomation.matching;

import com.invoiceautomation.config.GeneratorConfig;
import com.invoiceautomation.enums.GrnStatus;
import com.invoiceautomation.enums.MatchStatus;
import com.invoiceautomation.model.*;
import com.invoiceautomation.util.IdFactory;
import com.invoiceautomation.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ThreeWayMatcher {

    private final GeneratorConfig config;

    public ThreeWayMatcher() {
        this(GeneratorConfig.defaults());
    }

    public ThreeWayMatcher(GeneratorConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
    }

    /**
     * Backward-compatible overload.
     * When receipt headers are not provided, receipt lines are treated
     * as eligible for matching.
     */
    public List<MatchResult> match(
            List<Invoice> invoices,
            List<InvoiceLine> invoiceLines,
            List<PurchaseOrder> purchaseOrders,
            List<PurchaseOrderLine> poLines,
            List<GoodsReceiptLine> receiptLines,
            List<Vendor> vendors) {

        return match(
                invoices,
                invoiceLines,
                purchaseOrders,
                poLines,
                Collections.emptyList(),
                receiptLines,
                vendors
        );
    }

    /**
     * Matches invoice lines against purchase orders and eligible receipts.
     */
    public List<MatchResult> match(
            List<Invoice> invoices,
            List<InvoiceLine> invoiceLines,
            List<PurchaseOrder> purchaseOrders,
            List<PurchaseOrderLine> poLines,
            List<GoodsReceipt> receipts,
            List<GoodsReceiptLine> receiptLines,
            List<Vendor> vendors) {

        Map<String, PurchaseOrder> orders = purchaseOrders.stream()
                .collect(Collectors.toMap(
                        PurchaseOrder::poId, Function.identity()));

        Map<String, PurchaseOrderLine> poLinesById = poLines.stream()
                .collect(Collectors.toMap(
                        PurchaseOrderLine::poLineId, Function.identity()));

        Map<String, Vendor> vendorsById = vendors.stream()
                .collect(Collectors.toMap(
                        Vendor::vendorId, Function.identity()));

        Map<String, Invoice> invoicesById = invoices.stream()
                .collect(Collectors.toMap(
                        Invoice::invoiceId, Function.identity()));

        Map<String, GoodsReceipt> receiptsById = receipts.stream()
                .collect(Collectors.toMap(
                        GoodsReceipt::grnId, Function.identity()));

        Map<String, Integer> receivedQtyByPoLine = new HashMap<>();
        Map<String, String> firstReceiptLineByPoLine = new HashMap<>();

        for (GoodsReceiptLine receiptLine : receiptLines) {
            GoodsReceipt receipt = receiptsById.get(receiptLine.grnId());

            // When receipt headers are provided, only POSTED receipts count.
            if (!receipts.isEmpty()
                    && (receipt == null
                    || receipt.status() != GrnStatus.POSTED)) {
                continue;
            }

            receivedQtyByPoLine.merge(
                    receiptLine.poLineId(),
                    receiptLine.receivedQty(),
                    Integer::sum
            );

            firstReceiptLineByPoLine.putIfAbsent(
                    receiptLine.poLineId(),
                    receiptLine.grnLineId()
            );
        }

        List<MatchResult> results = new ArrayList<>();
        IdFactory ids = new IdFactory();

        for (InvoiceLine line : invoiceLines) {
            Invoice invoice = invoicesById.get(line.invoiceId());

            if (invoice == null) {
                throw new IllegalStateException(
                        "Invoice not found for invoice line: "
                                + line.invoiceLineId());
            }

            PurchaseOrder po = orders.get(invoice.poId());
            PurchaseOrderLine poLine = poLinesById.get(line.poLineId());
            Vendor vendor = vendorsById.get(invoice.vendorId());

            String poLineId = line.poLineId();

            String receiptLineId = poLineId == null
                    ? null
                    : firstReceiptLineByPoLine.get(poLineId);

            int receivedQty = poLineId == null
                    ? 0
                    : receivedQtyByPoLine.getOrDefault(poLineId, 0);

            BigDecimal priceVariance = BigDecimal.ZERO;
            BigDecimal taxVariance = BigDecimal.ZERO;
            int quantityVariance = 0;

            MatchStatus status;

            if (po == null) {
                status = MatchStatus.NO_PURCHASE_ORDER;

            } else if (poLine == null || vendor == null) {
                status = MatchStatus.ARITHMETIC_ERROR;

            } else if (invoice.invoiceDate() != null
                    && po.poDate() != null
                    && invoice.invoiceDate().isBefore(po.poDate())) {
                status = MatchStatus.DATE_ERROR;

            } else if (vendor.status() != null
                    && vendor.status().name().equalsIgnoreCase("CLOSED")) {
                status = MatchStatus.VENDOR_BLOCKED;

            } else if (invoice.subtotal() == null
                    || invoice.taxTotal() == null
                    || invoice.grandTotal() == null
                    || invoice.grandTotal()
                    .subtract(invoice.subtotal().add(invoice.taxTotal()))
                    .abs()
                    .compareTo(config.headerTolerance()) > 0) {
                status = MatchStatus.ARITHMETIC_ERROR;

            } else if (receivedQty == 0) {
                status = MatchStatus.MISSING_RECEIPT;

            } else {
                if (poLine.unitPrice() != null
                        && poLine.unitPrice().signum() > 0
                        && line.unitPrice() != null) {

                    priceVariance = line.unitPrice()
                            .subtract(poLine.unitPrice())
                            .multiply(new BigDecimal("100"))
                            .divide(
                                    poLine.unitPrice(),
                                    2,
                                    RoundingMode.HALF_UP
                            );
                }

                // Compare with the total received quantity.
                // Do not subtract quantities from previous invoices.
                quantityVariance = line.quantity() - receivedQty;

                if (line.taxAmount() != null
                        && line.lineAmount() != null
                        && poLine.taxCode() != null) {

                    BigDecimal expectedTax = Money.taxAmount(
                            line.lineAmount(),
                            poLine.taxCode()
                    );

                    taxVariance = line.taxAmount().subtract(expectedTax);
                }

                boolean taxCodeMismatch =
                        !Objects.equals(line.taxCode(), poLine.taxCode());

                boolean taxAmountMismatch =
                        taxVariance.abs()
                                .compareTo(config.taxTolerance()) > 0;

                if (taxCodeMismatch || taxAmountMismatch) {
                    status = MatchStatus.TAX_MISMATCH;

                } else if (priceVariance.abs().compareTo(
                        config.priceTolerance()
                                .multiply(new BigDecimal("100"))) > 0) {
                    status = MatchStatus.PRICE_VARIANCE;

                } else if (line.quantity() > poLine.orderedQty()
                        || line.quantity() > receivedQty) {
                    status = MatchStatus.QUANTITY_MISMATCH;

                } else {
                    status = MatchStatus.MATCHED;
                }
            }

            results.add(new MatchResult(
                    ids.nextMatchId(),
                    line.invoiceLineId(),
                    poLine == null ? null : poLine.poLineId(),
                    receiptLineId,
                    priceVariance,
                    quantityVariance,
                    taxVariance,
                    status
            ));
        }

        return results;
    }
}
