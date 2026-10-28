
package com.invoiceautomation.generator;

import com.invoiceautomation.config.GeneratorConfig;
import com.invoiceautomation.enums.IssueType;
import com.invoiceautomation.enums.LifecycleStatus;
import com.invoiceautomation.enums.MatchStatus;
import com.invoiceautomation.enums.SourceChannel;
import com.invoiceautomation.enums.TaxCode;
import com.invoiceautomation.enums.VendorStatus;
import com.invoiceautomation.model.GoodsReceipt;
import com.invoiceautomation.model.GoodsReceiptLine;
import com.invoiceautomation.model.GroundTruth;
import com.invoiceautomation.model.Invoice;
import com.invoiceautomation.model.InvoiceLine;
import com.invoiceautomation.model.PurchaseOrder;
import com.invoiceautomation.model.PurchaseOrderLine;
import com.invoiceautomation.model.Vendor;
import com.invoiceautomation.util.IdFactory;
import com.invoiceautomation.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class InvoiceGenerator {

    private final GeneratorConfig config;
    private final IdFactory ids;
    private final Random random;

    public InvoiceGenerator(GeneratorConfig config, IdFactory ids) {
        this.config = config;
        this.ids = ids;
        this.random = new Random(config.seed() ^ 0x5DEECE66DL);
    }

    public InvoiceData generate(SourceData source) {

        Map<String, Vendor> vendors = source.vendors().stream()
                .collect(Collectors.toMap(Vendor::vendorId, v -> v));

        Map<String, List<PurchaseOrderLine>> linesByPo =
                source.poLines().stream()
                        .collect(Collectors.groupingBy(PurchaseOrderLine::poId));

        Map<String, PurchaseOrder> orders = source.purchaseOrders().stream()
                .collect(Collectors.toMap(PurchaseOrder::poId, p -> p));

        Map<String, String> receiptToPo = source.receipts().stream()
                .collect(Collectors.toMap(GoodsReceipt::grnId, GoodsReceipt::poId));

        Map<String, Integer> receivedByLine = new HashMap<>();
        Map<String, Integer> receiptCountByPo = new HashMap<>();

        for (GoodsReceipt receipt : source.receipts()) {
            receiptCountByPo.merge(receipt.poId(), 1, Integer::sum);
        }

        for (GoodsReceiptLine line : source.receiptLines()) {
            if (receiptToPo.containsKey(line.grnId())) {
                receivedByLine.merge(
                        line.poLineId(), line.receivedQty(), Integer::sum);
            }
        }

        List<IssueType> scenarios = buildScenarios();

        List<Invoice> invoices = new ArrayList<>();
        List<InvoiceLine> invoiceLines = new ArrayList<>();
        List<GroundTruth> groundTruth = new ArrayList<>();

        Map<String, List<InvoiceLine>> linesByInvoice = new HashMap<>();
        List<Invoice> duplicateSources = new ArrayList<>();

        boolean headerMismatchInjected = false;
        boolean dateErrorInjected = false;

        int taxVendorSequence = 0;
        int cleanSequence = 0;

        for (int index = 0; index < scenarios.size(); index++) {

            IssueType issue = scenarios.get(index);

            Invoice invoice;
            String duplicateOf = null;
            List<String> tags = new ArrayList<>();
            List<InvoiceLine> currentLines = new ArrayList<>();

            if (issue == IssueType.DUPLICATE) {

                if (duplicateSources.isEmpty()) {
                    throw new IllegalStateException(
                            "Cannot create a duplicate without an original invoice.");
                }

                Invoice original = choose(duplicateSources);
                List<InvoiceLine> originalLines =
                        linesByInvoice.get(original.invoiceId());

                String invoiceId = ids.nextInvoiceId(original.vendorId());

                for (InvoiceLine originalLine : originalLines) {
                    currentLines.add(InvoiceLine.of(
                            invoiceId,
                            originalLine.lineNumber(),
                            originalLine.poLineId(),
                            originalLine.sku(),
                            originalLine.quantity(),
                            originalLine.unitPrice(),
                            originalLine.taxCode()));
                }

                BigDecimal subtotal = sumSubtotal(currentLines);
                BigDecimal tax = sumTax(currentLines);

                invoice = new Invoice(
                        invoiceId,
                        original.invoiceNumber(),
                        original.vendorId(),
                        original.poId(),
                        original.invoiceDate(),
                        original.dueDate(),
                        subtotal,
                        tax,
                        subtotal.add(tax),
                        original.sourceChannel(),
                        LifecycleStatus.RECEIVED,
                        null);

                duplicateOf = original.invoiceId();
                tags.add("DUPLICATE_INVOICE_NUMBER");
                tags.add("SAME_INVOICE_SUBMITTED_TWICE");

                groundTruth.add(new GroundTruth(
                        invoiceId,
                        IssueType.DUPLICATE,
                        tags,
                        duplicateOf,
                        MatchStatus.DUPLICATE,
                        false));

            } else {

                boolean closedVendorCase = false;
                boolean noPurchaseOrderCase = false;

                if (issue == IssueType.TAX_VENDOR_ERROR) {
                    int bucketSize = config.invoices()
                            * config.taxVendorErrorPct() / 100;
                    int position = taxVendorSequence++;

                    // Split the 60 configured cases into 30 closed-vendor
                    // cases and 30 tax-error cases. The first tax-error case
                    // is additionally used for the single DATE_ERROR fixture.
                    closedVendorCase = position < bucketSize / 2;
                } else if (issue == IssueType.CLEAN) {
                    // Preserve a dedicated no-PO edge case without reducing
                    // the required TAX_MISMATCH / VENDOR_BLOCKED counts.
                    noPurchaseOrderCase = cleanSequence == 1;
                    cleanSequence++;
                }

                final boolean closedCase = closedVendorCase;
                final boolean noPoCase = noPurchaseOrderCase;

                Predicate<PurchaseOrder> eligible = po -> {
                    Vendor vendor = vendors.get(po.vendorId());

                    if (vendor == null || linesByPo.get(po.poId()) == null) {
                        return false;
                    }

                    boolean hasReceipt =
                            receiptCountByPo.getOrDefault(po.poId(), 0) > 0;

                    if (issue == IssueType.MISSING_RECEIPT) {
                        return vendor.status() == VendorStatus.ACTIVE
                                && !hasReceipt;
                    }

                    if (closedCase) {
                        return vendor.status() == VendorStatus.CLOSED;
                    }

                    if (vendor.status() != VendorStatus.ACTIVE || !hasReceipt) {
                        return false;
                    }

                    return linesByPo.get(po.poId()).stream()
                            .anyMatch(line ->
                                    receivedByLine.getOrDefault(
                                            line.poLineId(), 0) > 0);
                };

                List<PurchaseOrder> candidates = source.purchaseOrders()
                        .stream()
                        .filter(eligible)
                        .toList();

                PurchaseOrder po = noPoCase ? null : choose(candidates);

                Vendor vendor;
                List<PurchaseOrderLine> candidateLines;

                if (po == null) {
                    vendor = choose(source.vendors().stream()
                            .filter(v -> v.status() == VendorStatus.ACTIVE)
                            .toList());

                    candidateLines = source.poLines().stream()
                            .filter(line -> line.sku() != null)
                            .toList();

                    tags.add("NO_PURCHASE_ORDER");
                } else {
                    vendor = vendors.get(po.vendorId());

                    candidateLines = linesByPo.get(po.poId()).stream()
                            .filter(line -> issue == IssueType.MISSING_RECEIPT
                                    || closedCase
                                    || receivedByLine.getOrDefault(
                                    line.poLineId(), 0) > 0)
                            .toList();
                }

                if (candidateLines.isEmpty()) {
                    throw new IllegalStateException(
                            "No invoice line source found for scenario: " + issue);
                }

                boolean forceSingleLine =
                        (issue == IssueType.CLEAN && !headerMismatchInjected)
                        || (issue == IssueType.TAX_VENDOR_ERROR
                        && !closedCase && !dateErrorInjected);

                List<PurchaseOrderLine> selectedLines = forceSingleLine
                        ? new ArrayList<>(candidateLines.subList(0, 1))
                        : selectMultipleLines(candidateLines);

                LocalDate invoiceDate = po == null
                        ? config.windowStart().plusDays(
                        random.nextInt((int) (
                                config.windowEnd().toEpochDay()
                                - config.windowStart().toEpochDay()
                                + 1)))
                        : po.poDate().plusDays(1 + random.nextInt(20));

                // FIX: Keep the expected label consistent with the
                // intentionally generated no-purchase-order scenario.
                MatchStatus expectedStatus = noPoCase
                        ? MatchStatus.NO_PURCHASE_ORDER
                        : MatchStatus.MATCHED;

                String invoiceId = ids.nextInvoiceId(vendor.vendorId());

                for (int lineIndex = 0;
                     lineIndex < selectedLines.size();
                     lineIndex++) {

                    PurchaseOrderLine sourceLine = selectedLines.get(lineIndex);

                    int receivedQty = receivedByLine.getOrDefault(
                            sourceLine.poLineId(), 0);

                    int quantity = po == null
                            ? 1 + random.nextInt(Math.max(
                            1, sourceLine.orderedQty()))
                            : receivedQty > 0
                              ? 1 + random.nextInt(Math.min(
                            receivedQty,
                            sourceLine.orderedQty()))
                              : sourceLine.orderedQty();

                    BigDecimal unitPrice = sourceLine.unitPrice();
                    TaxCode taxCode = sourceLine.taxCode();

                    // Inject line-level defects on the first line only.
                    if (lineIndex == 0) {
                        switch (issue) {
                            case CLEAN -> tags.add("CLEAN");

                            case PRICE_VARIANCE -> {
                                unitPrice = Money.round(unitPrice.multiply(
                                        new BigDecimal("1.05")));
                                tags.add("PRICE_VARIANCE");
                                expectedStatus = MatchStatus.PRICE_VARIANCE;
                            }

                            case QUANTITY_MISMATCH -> {
                                quantity = sourceLine.orderedQty() + 1;
                                tags.add("INVOICE_EXCEEDS_PO_QUANTITY");
                                expectedStatus = MatchStatus.QUANTITY_MISMATCH;
                            }

                            case MISSING_RECEIPT -> {
                                tags.add("MISSING_RECEIPT");
                                expectedStatus = MatchStatus.MISSING_RECEIPT;
                            }

                            case TAX_VENDOR_ERROR -> {
                                if (noPoCase) {
                                    tags.add("NO_PURCHASE_ORDER");
                                    expectedStatus = MatchStatus.NO_PURCHASE_ORDER;

                                } else if (closedCase) {
                                    tags.add("CLOSED_VENDOR");
                                    expectedStatus = MatchStatus.VENDOR_BLOCKED;

                                } else {
                                    TaxCode[] codes = TaxCode.values();

                                    taxCode = codes[
                                            (sourceLine.taxCode().ordinal() + 1)
                                                    % codes.length];

                                    tags.add("TAX_MISMATCH");
                                    expectedStatus = MatchStatus.TAX_MISMATCH;

                                    if (!dateErrorInjected) {
                                        invoiceDate = po.poDate().minusDays(1);
                                        dateErrorInjected = true;
                                        tags.add("INVOICE_DATE_BEFORE_PO_DATE");
                                        expectedStatus = MatchStatus.DATE_ERROR;
                                    }
                                }
                            }

                            default -> throw new IllegalStateException(
                                    "Unsupported scenario: " + issue);
                        }
                    }

                    String poLineId = po == null
                            ? null
                            : sourceLine.poLineId();

                    currentLines.add(InvoiceLine.of(
                            invoiceId,
                            lineIndex + 1,
                            poLineId,
                            sourceLine.sku(),
                            quantity,
                            unitPrice,
                            taxCode));
                }

                BigDecimal subtotal = sumSubtotal(currentLines);
                BigDecimal tax = sumTax(currentLines);
                BigDecimal grandTotal = subtotal.add(tax);

                if (issue == IssueType.CLEAN && !headerMismatchInjected) {
                    grandTotal = grandTotal.add(new BigDecimal("100.00"));
                    headerMismatchInjected = true;
                    tags.add("HEADER_TOTAL_MISMATCH");
                    expectedStatus = MatchStatus.ARITHMETIC_ERROR;
                }

                if (po != null) {
                    boolean partialDelivery = selectedLines.stream().anyMatch(line -> {
                        int received = receivedByLine.getOrDefault(
                                line.poLineId(), 0);
                        return received > 0 && received < line.orderedQty();
                    });

                    if (partialDelivery) {
                        tags.add("PARTIAL_DELIVERY");
                    }
                }

                if (vendor.status() == VendorStatus.CLOSED
                        && !tags.contains("CLOSED_VENDOR")) {
                    tags.add("CLOSED_VENDOR");
                }

                String invoiceNumber = String.format(
                        "BILL-%s-%06d",
                        vendor.vendorCode(),
                        index + 1);

                invoice = new Invoice(
                        invoiceId,
                        invoiceNumber,
                        vendor.vendorId(),
                        po == null ? null : po.poId(),
                        invoiceDate,
                        invoiceDate.plusDays(vendor.paymentTermsDays()),
                        subtotal,
                        tax,
                        grandTotal,
                        SourceChannel.values()[random.nextInt(
                                SourceChannel.values().length)],
                        LifecycleStatus.RECEIVED,
                        null);

                groundTruth.add(new GroundTruth(
                        invoiceId,
                        issue,
                        tags,
                        null,
                        expectedStatus,
                        expectedStatus == MatchStatus.MATCHED
                                && vendor.status() == VendorStatus.ACTIVE
                                && po != null));

                if (issue == IssueType.CLEAN
                        && expectedStatus == MatchStatus.MATCHED) {
                    duplicateSources.add(invoice);
                }
            }

            invoices.add(invoice);
            invoiceLines.addAll(currentLines);
            linesByInvoice.put(invoice.invoiceId(), currentLines);
        }

        if (invoices.size() != config.invoices()) {
            throw new IllegalStateException(
                    "Expected " + config.invoices()
                            + " invoices, generated " + invoices.size());
        }

        return new InvoiceData(invoices, invoiceLines, groundTruth);
    }

    private List<PurchaseOrderLine> selectMultipleLines(
            List<PurchaseOrderLine> candidates) {

        List<PurchaseOrderLine> available = new ArrayList<>(candidates);
        Collections.shuffle(available, random);

        int count = Math.min(available.size(), 1 + random.nextInt(3));
        return new ArrayList<>(available.subList(0, count));
    }

    private BigDecimal sumSubtotal(List<InvoiceLine> lines) {
        return lines.stream()
                .map(InvoiceLine::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumTax(List<InvoiceLine> lines) {
        return lines.stream()
                .map(InvoiceLine::taxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<IssueType> buildScenarios() {
        List<IssueType> scenarios = new ArrayList<>();

        int clean = config.invoices() * config.cleanPct() / 100;
        int price = config.invoices()
                * config.priceVariancePct() / 100;
        int quantity = config.invoices()
                * config.quantityMismatchPct() / 100;
        int duplicates = config.invoices()
                * config.duplicatePct() / 100;
        int missing = config.invoices()
                * config.missingReceiptPct() / 100;

        add(scenarios, IssueType.CLEAN, clean);
        add(scenarios, IssueType.PRICE_VARIANCE, price);
        add(scenarios, IssueType.QUANTITY_MISMATCH, quantity);
        add(scenarios, IssueType.MISSING_RECEIPT, missing);

        int taxVendor = config.invoices() - scenarios.size() - duplicates;
        add(scenarios, IssueType.TAX_VENDOR_ERROR, taxVendor);

        List<IssueType> nonDuplicates = new ArrayList<>(scenarios);
        Collections.shuffle(nonDuplicates, random);

        List<IssueType> result = new ArrayList<>(nonDuplicates);
        add(result, IssueType.DUPLICATE, duplicates);

        return result;
    }

    private static void add(
            List<IssueType> target, IssueType issue, int count) {

        for (int i = 0; i < count; i++) {
            target.add(issue);
        }
    }

    private <T> T choose(List<T> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException(
                    "No eligible records available for generation.");
        }

        return items.get(random.nextInt(items.size()));
    }
}
