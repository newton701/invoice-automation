package com.invoiceautomation.service;

import java.math.BigDecimal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.invoiceautomation.config.GeneratorConfig;
import com.invoiceautomation.entity.ApprovalEntity;
import com.invoiceautomation.entity.ExceptionCaseEntity;
import com.invoiceautomation.entity.GeneratedRecordEntity;
import com.invoiceautomation.entity.GoodsReceiptEntity;
import com.invoiceautomation.entity.GoodsReceiptLineEntity;
import com.invoiceautomation.entity.GroundTruthEntity;
import com.invoiceautomation.entity.InvoiceEntity;
import com.invoiceautomation.entity.InvoiceLineEntity;
import com.invoiceautomation.entity.InvoiceStatusEventEntity;
import com.invoiceautomation.entity.MatchResultEntity;
import com.invoiceautomation.entity.PaymentEntity;
import com.invoiceautomation.entity.PurchaseOrderEntity;
import com.invoiceautomation.entity.PurchaseOrderLineEntity;
import com.invoiceautomation.entity.VendorEntity;
import com.invoiceautomation.enums.*;
import com.invoiceautomation.export.DatasetExporter;
import com.invoiceautomation.extraction.InvoiceDocumentExtractor;
import com.invoiceautomation.generator.*;
import com.invoiceautomation.matching.ThreeWayMatcher;
import com.invoiceautomation.model.*;
import com.invoiceautomation.repository.ApprovalRepository;
import com.invoiceautomation.repository.ExceptionCaseRepository;
import com.invoiceautomation.repository.GeneratedRecordRepository;
import com.invoiceautomation.repository.GoodsReceiptLineRepository;
import com.invoiceautomation.repository.GoodsReceiptRepository;
import com.invoiceautomation.repository.GroundTruthRepository;
import com.invoiceautomation.repository.InvoiceLineRepository;
import com.invoiceautomation.repository.InvoiceRepository;
import com.invoiceautomation.repository.InvoiceStatusEventRepository;
import com.invoiceautomation.repository.MatchResultRepository;
import com.invoiceautomation.repository.PaymentRepository;
import com.invoiceautomation.repository.PurchaseOrderLineRepository;
import com.invoiceautomation.repository.PurchaseOrderRepository;
import com.invoiceautomation.repository.VendorRepository;
import com.invoiceautomation.util.IdFactory;
import com.invoiceautomation.util.Money;
import com.invoiceautomation.validation.DuplicateInvoiceDetector;
import com.invoiceautomation.validation.ValidationReport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PipelineService {

    private static final LocalDate PROCESSING_DATE =
            LocalDate.of(2026, 10, 22);

    private static final LocalDate PAYMENT_SCHEDULE_DATE =
            LocalDate.of(2026, 10, 30);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final GeneratedRecordRepository generatedRecordRepository;
    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final GoodsReceiptLineRepository goodsReceiptLineRepository;
    private final GroundTruthRepository groundTruthRepository;
    private final MatchResultRepository matchResultRepository;
    private final ExceptionCaseRepository exceptionCaseRepository;
    private final ApprovalRepository approvalRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceStatusEventRepository invoiceStatusEventRepository;

    private final ObjectMapper mapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Value("${invoice.output-dir:data/output}")
    private String outputDirectory;

    public PipelineService(
            InvoiceRepository invoiceRepository,
            InvoiceLineRepository invoiceLineRepository,
            GeneratedRecordRepository generatedRecordRepository,
            VendorRepository vendorRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderLineRepository purchaseOrderLineRepository,
            GoodsReceiptRepository goodsReceiptRepository,
            GoodsReceiptLineRepository goodsReceiptLineRepository,
            GroundTruthRepository groundTruthRepository,
            MatchResultRepository matchResultRepository,
            ExceptionCaseRepository exceptionCaseRepository,
            ApprovalRepository approvalRepository,
            PaymentRepository paymentRepository,
            InvoiceStatusEventRepository invoiceStatusEventRepository) {

        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.generatedRecordRepository = generatedRecordRepository;
        this.vendorRepository = vendorRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderLineRepository = purchaseOrderLineRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.goodsReceiptLineRepository = goodsReceiptLineRepository;
        this.groundTruthRepository = groundTruthRepository;
        this.matchResultRepository = matchResultRepository;
        this.exceptionCaseRepository = exceptionCaseRepository;
        this.approvalRepository = approvalRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceStatusEventRepository = invoiceStatusEventRepository;
    }

    @Transactional
    public PipelineSummary runPipeline() throws IOException {

        GeneratorConfig config = GeneratorConfig.defaults();

        IdFactory ids = new IdFactory();

        SourceData source = new SourceDataGenerator(
                config,
                ids,
                new Random(config.seed())
        ).generate();

        InvoiceData generated =
                new InvoiceGenerator(config, ids).generate(source);

        // Match invoice lines against PO lines and posted receipts.
        // Keep only this declaration of rawMatches.
        List<MatchResult> rawMatches = new ThreeWayMatcher(config).match(
                generated.invoices(),
                generated.invoiceLines(),
                source.purchaseOrders(),
                source.poLines(),
                source.receipts(),
                source.receiptLines(),
                source.vendors()
        );

        Set<String> duplicateIds =
                new DuplicateInvoiceDetector()
                        .findDuplicateInvoiceIds(generated.invoices());

        Map<String, String> invoiceByLine =
                generated.invoiceLines().stream()
                        .collect(Collectors.toMap(
                                InvoiceLine::invoiceLineId,
                                InvoiceLine::invoiceId
                        ));

        Map<String, MatchStatus> statusByInvoice = new HashMap<>();

        Map<String, List<MatchStatus>> statusesByInvoice =
                new HashMap<>();

        List<MatchResult> matches = new ArrayList<>();

        for (MatchResult match : rawMatches) {

            String invoiceId =
                    invoiceByLine.get(match.invoiceLineId());

            if (invoiceId == null) {
                throw new IllegalStateException(
                        "No invoice found for line: "
                                + match.invoiceLineId()
                );
            }

            MatchStatus status = duplicateIds.contains(invoiceId)
                    ? MatchStatus.DUPLICATE
                    : match.lineMatchStatus();

            statusesByInvoice
                    .computeIfAbsent(invoiceId, key -> new ArrayList<>())
                    .add(status);

            matches.add(new MatchResult(
                    match.matchId(),
                    match.invoiceLineId(),
                    match.poLineId(),
                    match.grnLineId(),
                    match.priceVariancePct(),
                    match.qtyVariance(),
                    match.taxVariance(),
                    status
            ));
        }

        for (Map.Entry<String, List<MatchStatus>> entry
                : statusesByInvoice.entrySet()) {

            List<MatchStatus> lineStatuses = entry.getValue();

            MatchStatus invoiceStatus;

            if (lineStatuses.contains(MatchStatus.DUPLICATE)) {
                invoiceStatus = MatchStatus.DUPLICATE;
            } else {
                invoiceStatus = lineStatuses.stream()
                        .filter(status -> status != MatchStatus.MATCHED)
                        .min(Comparator.comparingInt(
                                this::matchStatusPriority
                        ))
                        .orElse(MatchStatus.MATCHED);
            }

            statusByInvoice.put(entry.getKey(), invoiceStatus);
        }

        Map<String, Vendor> vendors =
                source.vendors().stream()
                        .collect(Collectors.toMap(
                                Vendor::vendorId,
                                v -> v
                        ));

        Map<String, PurchaseOrder> orders =
                source.purchaseOrders().stream()
                        .collect(Collectors.toMap(
                                PurchaseOrder::poId,
                                p -> p
                        ));

        List<Invoice> processedInvoices = new ArrayList<>();
        List<ExceptionCase> exceptions = new ArrayList<>();
        List<Approval> approvals = new ArrayList<>();
        List<Payment> payments = new ArrayList<>();
        List<InvoiceStatusEvent> events = new ArrayList<>();

        IdFactory workflowIds = new IdFactory();

        for (Invoice original : generated.invoices()) {

            MatchStatus status = statusByInvoice.getOrDefault(
                    original.invoiceId(),
                    MatchStatus.ARITHMETIC_ERROR
            );

            Invoice processed = original.withMatchStatus(status);

            Vendor vendor = vendors.get(original.vendorId());

            LifecycleStatus lifecycle;

            if (status == MatchStatus.MATCHED
                    && vendor != null
                    && vendor.status() == VendorStatus.ACTIVE) {

                lifecycle = LifecycleStatus.PAYMENT_READY;

                approvals.add(new Approval(
                        workflowIds.nextApprovalId(),
                        original.invoiceId(),
                        ApproverRole.PROCESS_OWNER,
                        ApprovalDecision.APPROVED,
                        PROCESSING_DATE
                ));

                approvals.add(new Approval(
                        workflowIds.nextApprovalId(),
                        original.invoiceId(),
                        ApproverRole.APPROVER,
                        ApprovalDecision.APPROVED,
                        PROCESSING_DATE
                ));

                payments.add(new Payment(
                        workflowIds.nextPaymentId(),
                        original.invoiceId(),
                        PaymentStatus.PAYMENT_READY,
                        PAYMENT_SCHEDULE_DATE,
                        null,
                        original.grandTotal()
                ));

            } else {

                lifecycle = status == MatchStatus.VENDOR_BLOCKED
                        ? LifecycleStatus.BLOCKED
                        : LifecycleStatus.EXCEPTION;

                exceptions.add(new ExceptionCase(
                        workflowIds.nextCaseId(),
                        original.invoiceId(),
                        status,
                        route(status),
                        CaseStatus.OPEN,
                        PROCESSING_DATE
                ));
            }

            processed = processed.withLifecycleStatus(lifecycle);
            processedInvoices.add(processed);

            events.add(new InvoiceStatusEvent(
                    workflowIds.nextEventId(),
                    original.invoiceId(),
                    null,
                    LifecycleStatus.RECEIVED,
                    PROCESSING_DATE,
                    "Invoice intake"
            ));

            if (lifecycle == LifecycleStatus.PAYMENT_READY) {

                events.add(new InvoiceStatusEvent(
                        workflowIds.nextEventId(),
                        original.invoiceId(),
                        LifecycleStatus.RECEIVED,
                        LifecycleStatus.MATCHED,
                        PROCESSING_DATE,
                        "Three-way match passed"
                ));

                events.add(new InvoiceStatusEvent(
                        workflowIds.nextEventId(),
                        original.invoiceId(),
                        LifecycleStatus.MATCHED,
                        LifecycleStatus.PAYMENT_READY,
                        PROCESSING_DATE,
                        "Required approvals recorded; payment eligible"
                ));

            } else {

                events.add(new InvoiceStatusEvent(
                        workflowIds.nextEventId(),
                        original.invoiceId(),
                        LifecycleStatus.RECEIVED,
                        lifecycle,
                        PROCESSING_DATE,
                        "Rule-engine result: " + status.name()
                ));
            }
        }

        InvoiceData processedData = new InvoiceData(
                processedInvoices,
                generated.invoiceLines(),
                generated.groundTruth()
        );

        ValidationReport report = validate(
                config,
                source,
                processedData,
                matches,
                exceptions,
                approvals,
                payments,
                events,
                duplicateIds
        );

        Map<String, Integer> recordCounts = new LinkedHashMap<>();

        recordCounts.put("vendors", source.vendors().size());
        recordCounts.put("purchase_orders", source.purchaseOrders().size());
        recordCounts.put("po_lines", source.poLines().size());
        recordCounts.put("goods_receipts", source.receipts().size());
        recordCounts.put("goods_receipt_lines", source.receiptLines().size());
        recordCounts.put("invoices", processedData.invoices().size());
        recordCounts.put("invoice_lines", processedData.invoiceLines().size());
        recordCounts.put("ground_truth", processedData.groundTruth().size());
        recordCounts.put("match_results", matches.size());
        recordCounts.put("exception_cases", exceptions.size());
        recordCounts.put("approvals", approvals.size());
        recordCounts.put("payments", payments.size());
        recordCounts.put("invoice_status_events", events.size());

        Map<String, Integer> scenarios =
                processedData.groundTruth().stream()
                        .collect(Collectors.groupingBy(
                                g -> g.issueType().name(),
                                TreeMap::new,
                                Collectors.summingInt(g -> 1)
                        ));

        Map<String, Integer> matchCounts =
                processedData.invoices().stream()
                        .collect(Collectors.groupingBy(
                                i -> i.matchStatus() == null
                                        ? "UNMATCHED"
                                        : i.matchStatus().name(),
                                TreeMap::new,
                                Collectors.summingInt(i -> 1)
                        ));

        String checksum = checksum(List.of(
                source,
                processedData,
                matches,
                exceptions,
                approvals,
                payments,
                events
        ));

        PipelineSummary summary = new PipelineSummary(
                report.passed(),
                recordCounts,
                scenarios,
                matchCounts,
                exceptions.size(),
                approvals.size(),
                payments.size(),
                checksum,
                outputDirectory,
                report
        );

        exportInvoiceDocuments(processedData, Path.of(outputDirectory));

        new DatasetExporter().exportAll(
                source,
                processedData,
                matches,
                exceptions,
                approvals,
                payments,
                events,
                Path.of(outputDirectory),
                summary
        );

        writeValidationReport(
                report,
                Path.of(outputDirectory).resolve("validation_summary.md")
        );

        persistAll(
                source,
                processedData,
                matches,
                exceptions,
                approvals,
                payments,
                events
        );

        return summary;
    }

    private RouteTarget route(MatchStatus status) {
        return switch (status) {
            case PRICE_VARIANCE, TAX_MISMATCH,
                 ARITHMETIC_ERROR, DATE_ERROR ->
                    RouteTarget.OPERATIONS_ANALYST;

            case QUANTITY_MISMATCH, MISSING_RECEIPT ->
                    RouteTarget.PROCESS_OWNER;

            case DUPLICATE, NO_PURCHASE_ORDER ->
                    RouteTarget.SYSTEM_ADMIN;

            case VENDOR_BLOCKED ->
                    RouteTarget.APPROVER;

            case MATCHED ->
                    RouteTarget.OPERATIONS_ANALYST;
        };
    }

    private int matchStatusPriority(MatchStatus status) {
        return switch (status) {
            case DUPLICATE -> 0;
            case VENDOR_BLOCKED -> 1;
            case DATE_ERROR -> 2;
            case ARITHMETIC_ERROR -> 3;
            case NO_PURCHASE_ORDER -> 4;
            case TAX_MISMATCH -> 5;
            case PRICE_VARIANCE -> 6;
            case QUANTITY_MISMATCH -> 7;
            case MISSING_RECEIPT -> 8;
            case MATCHED -> 100;
        };
    }

    private ValidationReport validate(
            GeneratorConfig c,
            SourceData s,
            InvoiceData i,
            List<MatchResult> matches,
            List<ExceptionCase> exceptions,
            List<Approval> approvals,
            List<Payment> payments,
            List<InvoiceStatusEvent> events,
            Set<String> duplicateIds) {

        ValidationReport.Builder b = new ValidationReport.Builder();

        b.check(s.vendors().size() == c.vendors(),
                "Vendor count differs from configured volume");

        b.check(s.purchaseOrders().size() == c.purchaseOrders(),
                "PO count differs from configured volume");

        b.check(s.poLines().size() == c.poLines(),
                "PO line count differs from configured volume");

        b.check(s.receipts().size() == c.receipts(),
                "Receipt count differs from configured volume");

        b.check(i.invoices().size() == c.invoices(),
                "Invoice count differs from configured volume");

        b.check(i.groundTruth().size() == c.invoices(),
                "Ground truth must contain one label per invoice");

        b.check(unique(s.vendors().stream()
                        .map(Vendor::vendorId).toList()),
                "Vendor primary keys are not unique");

        b.check(unique(s.purchaseOrders().stream()
                        .map(PurchaseOrder::poId).toList()),
                "PO primary keys are not unique");

        b.check(unique(s.receipts().stream()
                        .map(GoodsReceipt::grnId).toList()),
                "Goods-receipt primary keys are not unique");

        b.check(unique(s.vendors().stream()
                        .map(Vendor::vendorCode).toList()),
                "Vendor codes are not unique");

        b.check(unique(s.poLines().stream()
                        .map(PurchaseOrderLine::poLineId).toList()),
                "PO-line primary keys are not unique");

        b.check(unique(i.invoices().stream()
                        .map(Invoice::invoiceId).toList()),
                "Invoice primary keys are not unique");

        b.check(unique(s.receiptLines().stream()
                        .map(GoodsReceiptLine::grnLineId).toList()),
                "Receipt-line primary keys are not unique");

        b.check(unique(i.invoiceLines().stream()
                        .map(InvoiceLine::invoiceLineId).toList()),
                "Invoice-line primary keys are not unique");

        Set<String> vendorIds = s.vendors().stream()
                .map(Vendor::vendorId)
                .collect(Collectors.toSet());

        Set<String> poIds = s.purchaseOrders().stream()
                .map(PurchaseOrder::poId)
                .collect(Collectors.toSet());

        Set<String> poLineIds = s.poLines().stream()
                .map(PurchaseOrderLine::poLineId)
                .collect(Collectors.toSet());

        Set<String> receiptIds = s.receipts().stream()
                .map(GoodsReceipt::grnId)
                .collect(Collectors.toSet());

        Set<String> invoiceIds = i.invoices().stream()
                .map(Invoice::invoiceId)
                .collect(Collectors.toSet());

        Map<String, PurchaseOrder> ordersById =
                s.purchaseOrders().stream()
                        .collect(Collectors.toMap(
                                PurchaseOrder::poId, x -> x
                        ));

        Map<String, PurchaseOrderLine> poLinesById =
                s.poLines().stream()
                        .collect(Collectors.toMap(
                                PurchaseOrderLine::poLineId, x -> x
                        ));

        Map<String, GoodsReceipt> receiptsById =
                s.receipts().stream()
                        .collect(Collectors.toMap(
                                GoodsReceipt::grnId, x -> x
                        ));

        Map<String, Vendor> vendorsById =
                s.vendors().stream()
                        .collect(Collectors.toMap(
                                Vendor::vendorId, x -> x
                        ));

        b.check(s.purchaseOrders().stream()
                        .allMatch(p -> vendorIds.contains(p.vendorId())),
                "PO has an unknown vendor reference");

        b.check(s.poLines().stream()
                        .allMatch(p -> poIds.contains(p.poId())),
                "PO line has an unknown PO reference");

        b.check(s.receipts().stream()
                        .allMatch(r -> poIds.contains(r.poId())),
                "Receipt has an unknown PO reference");

        b.check(s.receiptLines().stream()
                        .allMatch(r -> receiptIds.contains(r.grnId())
                                && poLineIds.contains(r.poLineId())),
                "Receipt line has an invalid receipt/PO-line reference");

        b.check(s.receiptLines().stream().allMatch(r ->
                        receiptsById.get(r.grnId()).poId()
                                .equals(poLinesById.get(r.poLineId()).poId())),
                "Receipt line links a receipt and PO line from different POs");

        b.check(s.purchaseOrders().stream()
                        .allMatch(p -> !p.expectedDeliveryDate()
                                .isBefore(p.poDate())),
                "PO expected delivery date precedes PO date");

        b.check(s.receipts().stream()
                        .allMatch(r -> !r.receiptDate()
                                .isBefore(ordersById.get(r.poId()).poDate())),
                "Receipt date precedes its PO date");

        b.check(s.purchaseOrders().stream().allMatch(p ->
                        p.totalAmount().compareTo(
                                s.poLines().stream()
                                        .filter(l -> l.poId().equals(p.poId()))
                                        .map(PurchaseOrderLine::lineAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        ) == 0),
                "PO header total does not equal sum of PO line amounts");

        b.check(s.purchaseOrders().stream()
                        .allMatch(p -> p.poDate().compareTo(
                                vendorsById.get(p.vendorId()).onboardedDate()
                        ) >= 0),
                "PO date precedes vendor onboarding");

        b.check(i.invoices().stream()
                        .allMatch(x -> vendorIds.contains(x.vendorId())
                                && (x.poId() == null || poIds.contains(x.poId()))),
                "Invoice has an invalid vendor/PO reference");

        b.check(i.invoiceLines().stream()
                        .allMatch(x -> invoiceIds.contains(x.invoiceId())
                                && (x.poLineId() == null
                                || poLineIds.contains(x.poLineId()))),
                "Invoice line has an invalid invoice/PO-line reference");

        b.check(i.invoiceLines().stream().allMatch(x ->
                        Money.lineAmount(x.quantity(), x.unitPrice())
                                .compareTo(x.lineAmount()) == 0
                                && Money.taxAmount(x.lineAmount(), x.taxCode())
                                .compareTo(x.taxAmount()) == 0
                                && x.lineAmount().add(x.taxAmount())
                                .compareTo(x.lineTotal()) == 0),
                "Invoice line arithmetic reconciliation failed");

        Map<String, GroundTruth> truthByInvoice =
                i.groundTruth().stream()
                        .collect(Collectors.toMap(
                                GroundTruth::invoiceId, x -> x
                        ));

        b.check(truthByInvoice.keySet().equals(invoiceIds),
                "Ground truth must have exactly one label for each invoice");

        b.check(i.invoices().stream().allMatch(inv ->
                        truthByInvoice.containsKey(inv.invoiceId())
                                && inv.matchStatus()
                                == truthByInvoice.get(inv.invoiceId())
                                .expectedMatchStatus()),
                "Actual match statuses differ from independent ground-truth labels");

        b.check(i.invoices().stream().allMatch(inv ->
                        inv.grandTotal().compareTo(
                                inv.subtotal().add(inv.taxTotal())
                        ) == 0
                                || (truthByInvoice.containsKey(inv.invoiceId())
                                && truthByInvoice.get(inv.invoiceId())
                                .edgeCaseTags()
                                .contains("HEADER_TOTAL_MISMATCH"))),
                "Unlabelled invoice header arithmetic mismatch found");

        b.check(i.invoices().stream()
                        .allMatch(inv -> inv.invoiceNumber()
                                .matches("[A-Z0-9-]{6,20}")),
                "Invoice number format/length violates the data dictionary");

        b.check(i.invoices().stream().allMatch(inv ->
                        inv.dueDate().equals(
                                inv.invoiceDate().plusDays(
                                        vendorsById.get(inv.vendorId())
                                                .paymentTermsDays()
                                )
                        )),
                "Invoice due date does not follow vendor payment terms");

        b.check(i.groundTruth().stream()
                        .allMatch(g -> invoiceIds.contains(g.invoiceId())),
                "Ground truth has an unknown invoice ID");

        b.check(matches.size() == i.invoiceLines().size(),
                "Every invoice line must have exactly one match result");

        b.check(unique(matches.stream()
                        .map(MatchResult::matchId).toList()),
                "Match-result primary keys are not unique");

        b.check(unique(exceptions.stream()
                        .map(ExceptionCase::caseId).toList()),
                "Exception-case primary keys are not unique");

        b.check(unique(approvals.stream()
                        .map(Approval::approvalId).toList()),
                "Approval primary keys are not unique");

        b.check(unique(payments.stream()
                        .map(Payment::paymentId).toList()),
                "Payment primary keys are not unique");

        b.check(unique(events.stream()
                        .map(InvoiceStatusEvent::eventId).toList()),
                "Status-event primary keys are not unique");

        b.check(s.vendors().stream().allMatch(v ->
                        v.vendorId().matches("VND-\\d{5}")
                                && v.vendorCode().matches("\\d{5}")),
                "Vendor ID/code format is invalid");

        b.check(s.purchaseOrders().stream()
                        .allMatch(p -> p.poId().matches("PO-2026-\\d{6}")),
                "Purchase-order ID format is invalid");

        b.check(s.receipts().stream()
                        .allMatch(r -> r.grnId().matches("GRN-2026-\\d{6}")),
                "Goods-receipt ID format is invalid");

        b.check(i.invoices().stream()
                        .allMatch(inv -> inv.invoiceId()
                                .matches("INV-\\d{5}-2026-\\d{6}")),
                "Invoice ID format is invalid");

        b.check(duplicateIds.size()
                        == c.invoices() * c.duplicatePct() / 100,
                "Duplicate detector did not identify expected duplicate submissions");

        Set<String> labelledDuplicateIds =
                i.groundTruth().stream()
                        .filter(g -> g.issueType() == IssueType.DUPLICATE)
                        .map(GroundTruth::invoiceId)
                        .collect(Collectors.toSet());

        b.check(duplicateIds.equals(labelledDuplicateIds),
                "Duplicate detector IDs differ from the separately stored labels");

        Map<String, Invoice> invoicesById =
                i.invoices().stream()
                        .collect(Collectors.toMap(
                                Invoice::invoiceId, x -> x
                        ));

        b.check(i.groundTruth().stream()
                        .filter(g -> g.issueType() == IssueType.DUPLICATE)
                        .allMatch(g -> {
                            Invoice duplicate = invoicesById.get(g.invoiceId());
                            Invoice original = invoicesById.get(
                                    g.duplicateOfInvoiceId()
                            );

                            return duplicate != null
                                    && original != null
                                    && duplicate.vendorId()
                                    .equals(original.vendorId())
                                    && duplicate.invoiceNumber()
                                    .equals(original.invoiceNumber());
                        }),
                "Ground-truth duplicate does not point to the correct vendor/invoice-number pair");

        b.check(i.groundTruth().stream()
                        .filter(g -> g.issueType() == IssueType.CLEAN)
                        .count() == c.invoices() * c.cleanPct() / 100,
                "CLEAN scenario count differs from brief");

        b.check(i.groundTruth().stream()
                        .filter(g -> g.issueType() == IssueType.PRICE_VARIANCE)
                        .count() == c.invoices() * c.priceVariancePct() / 100,
                "PRICE_VARIANCE scenario count differs from brief");

        b.check(i.groundTruth().stream()
                        .filter(g -> g.issueType() == IssueType.QUANTITY_MISMATCH)
                        .count() == c.invoices() * c.quantityMismatchPct() / 100,
                "QUANTITY_MISMATCH scenario count differs from brief");

        b.check(i.groundTruth().stream()
                        .filter(g -> g.issueType() == IssueType.DUPLICATE)
                        .count() == c.invoices() * c.duplicatePct() / 100,
                "DUPLICATE scenario count differs from brief");

        b.check(i.groundTruth().stream()
                        .filter(g -> g.issueType() == IssueType.MISSING_RECEIPT)
                        .count() == c.invoices() * c.missingReceiptPct() / 100,
                "MISSING_RECEIPT scenario count differs from brief");

        b.check(i.groundTruth().stream()
                        .filter(g -> g.issueType() == IssueType.TAX_VENDOR_ERROR)
                        .count() == c.invoices() * c.taxVendorErrorPct() / 100,
                "TAX_VENDOR_ERROR scenario count differs from brief");

        List<String> tags = i.groundTruth().stream()
                .flatMap(g -> g.edgeCaseTags().stream())
                .toList();

        b.check(tags.contains("DUPLICATE_INVOICE_NUMBER"),
                "Duplicate invoice-number edge case is missing");

        b.check(tags.contains("SAME_INVOICE_SUBMITTED_TWICE"),
                "Same-invoice resubmission edge case is missing");

        b.check(tags.contains("HEADER_TOTAL_MISMATCH"),
                "Header/line total mismatch edge case is missing");

        b.check(tags.contains("INVOICE_DATE_BEFORE_PO_DATE"),
                "Invoice-before-PO date edge case is missing");

        b.check(tags.contains("PARTIAL_DELIVERY"),
                "Partial-delivery edge case is missing");

        b.check(tags.contains("CLOSED_VENDOR"),
                "Closed-vendor edge case is missing");

        b.check(tags.contains("TAX_MISMATCH"),
                "Tax-mismatch edge case is missing");

        b.check(tags.contains("INVOICE_EXCEEDS_PO_QUANTITY"),
                "Over-PO-quantity edge case is missing");

        long expectedReady = i.groundTruth().stream()
                .filter(GroundTruth::expectedPaymentReady)
                .count();

        b.check(expectedReady == payments.size(),
                "Rule-engine payment-ready count differs from independently generated ground truth");

        Set<String> expectedReadyIds =
                i.groundTruth().stream()
                        .filter(GroundTruth::expectedPaymentReady)
                        .map(GroundTruth::invoiceId)
                        .collect(Collectors.toSet());

        Set<String> actualReadyIds =
                payments.stream()
                        .map(Payment::invoiceId)
                        .collect(Collectors.toSet());

        b.check(expectedReadyIds.equals(actualReadyIds),
                "Rule-engine payment-ready invoice IDs differ from independent ground truth");

        b.check(payments.stream().allMatch(p ->
                        i.invoices().stream().anyMatch(inv ->
                                inv.invoiceId().equals(p.invoiceId())
                                        && inv.lifecycleStatus()
                                        == LifecycleStatus.PAYMENT_READY
                                        && inv.matchStatus() == MatchStatus.MATCHED
                                        && p.amount().compareTo(inv.grandTotal()) == 0)),
                "A blocked/exception invoice is payment-ready or payment amount is inconsistent");

        b.check(approvals.size() == payments.size() * 2,
                "Each payment-ready invoice must have both required approvals");

        b.check(events.size()
                        == i.invoices().size() + payments.size() * 2
                        + exceptions.size(),
                "Invoice status-history event count does not match the workflow transitions");

        b.check(events.stream().allMatch(e ->
                        (e.fromStatus() == null
                                && e.toStatus() == LifecycleStatus.RECEIVED)
                                || (e.fromStatus() == LifecycleStatus.RECEIVED
                                && (e.toStatus() == LifecycleStatus.MATCHED
                                || e.toStatus() == LifecycleStatus.EXCEPTION
                                || e.toStatus() == LifecycleStatus.BLOCKED))
                                || (e.fromStatus() == LifecycleStatus.MATCHED
                                && e.toStatus() == LifecycleStatus.PAYMENT_READY)),
                "Illegal lifecycle transition found in status history");

        b.check(approvals.stream().allMatch(a ->
                        i.invoices().stream().anyMatch(inv ->
                                inv.invoiceId().equals(a.invoiceId())
                                        && inv.matchStatus() == MatchStatus.MATCHED)),
                "Approval exists for an ineligible invoice");

        b.check(exceptions.stream().allMatch(e ->
                        i.invoices().stream().anyMatch(inv ->
                                inv.invoiceId().equals(e.invoiceId())
                                        && inv.matchStatus() != MatchStatus.MATCHED)),
                "Exception exists for a matched invoice");

        if (i.groundTruth().stream()
                .noneMatch(g -> g.edgeCaseTags()
                        .contains("HEADER_TOTAL_MISMATCH"))) {
            b.warn("No invoice header total mismatch tag was found.");
        }

        return b.build();
    }

    private boolean unique(List<String> values) {
        return new HashSet<>(values).size() == values.size();
    }

    /**
     * Recursively compares document values.
     * BigDecimal values are compared numerically to avoid scale-only mismatches.
     */
    private boolean documentValuesEqual(Object expected, Object actual) {

        if (expected instanceof BigDecimal expectedNumber
                && actual instanceof Number actualNumber) {

            return expectedNumber.compareTo(
                    new BigDecimal(actualNumber.toString())
            ) == 0;
        }

        if (expected instanceof Number expectedNumber
                && actual instanceof Number actualNumber) {

            return new BigDecimal(expectedNumber.toString())
                    .compareTo(new BigDecimal(actualNumber.toString())) == 0;
        }

        if (expected instanceof Map<?, ?> expectedMap
                && actual instanceof Map<?, ?> actualMap) {

            if (!expectedMap.keySet().equals(actualMap.keySet())) {
                return false;
            }

            for (Object key : expectedMap.keySet()) {
                if (!documentValuesEqual(
                        expectedMap.get(key),
                        actualMap.get(key))) {
                    return false;
                }
            }

            return true;
        }

        if (expected instanceof List<?> expectedList
                && actual instanceof List<?> actualList) {

            if (expectedList.size() != actualList.size()) {
                return false;
            }

            for (int i = 0; i < expectedList.size(); i++) {
                if (!documentValuesEqual(
                        expectedList.get(i),
                        actualList.get(i))) {
                    return false;
                }
            }

            return true;
        }

        if (expected instanceof java.time.LocalDate expectedDate
                && actual instanceof String actualDate) {
            return expectedDate.toString().equals(actualDate);
        }
        if (expected instanceof Enum<?> expectedEnum
                && actual instanceof String actualString) {
            return expectedEnum.name().equals(actualString);
        }
        return Objects.equals(expected, actual);
    }

    private void exportInvoiceDocuments(
            InvoiceData data,
            Path root) throws IOException {

        Files.createDirectories(root);

        Path documents = root.resolve("invoice_documents.jsonl");

        Map<String, List<InvoiceLine>> linesByInvoice =
                data.invoiceLines().stream()
                        .collect(Collectors.groupingBy(
                                InvoiceLine::invoiceId
                        ));

        InvoiceDocumentExtractor extractor =
                new InvoiceDocumentExtractor();

        int extracted = 0;
        List<String> mismatches = new ArrayList<>();
        StringBuilder jsonl = new StringBuilder();

        for (Invoice invoice : data.invoices()) {

            List<InvoiceLine> lines =
                    linesByInvoice.get(invoice.invoiceId());

            if (lines == null || lines.isEmpty()) {
                mismatches.add(
                        invoice.invoiceId() + ": missing lines"
                );
                continue;
            }

            Map<String, Object> document =
                    extractor.createDocument(invoice, lines);

            String json = extractor.toJson(document);

            jsonl.append(json).append('\n');

            Map<String, Object> fields =
                    extractor.extractJson(json);

            // Compare every extracted field, including nested line data.
            if (!documentValuesEqual(document, fields)) {
                mismatches.add(
                        invoice.invoiceId()
                                + ": extracted document fields differ from source"
                );
            } else {
                extracted++;
            }
        }

        Files.writeString(
                documents,
                jsonl.toString(),
                StandardCharsets.UTF_8
        );

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("documents_created", data.invoices().size());
        summary.put("documents_extracted", extracted);
        summary.put("mismatches", mismatches);

        mapper.writerWithDefaultPrettyPrinter().writeValue(
                root.resolve("extraction_summary.json").toFile(),
                summary
        );

        if (!mismatches.isEmpty()) {
            throw new IllegalStateException(
                    "Invoice document extraction validation failed: "
                            + mismatches.size() + " mismatch(es)"
            );
        }
    }

    private String checksum(Object data) {
        try {
            byte[] bytes = mapper.writeValueAsBytes(data);

            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(bytes);

            StringBuilder hex = new StringBuilder();

            for (byte x : digest) {
                hex.append(String.format("%02x", x));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not compute dataset checksum",
                    e
            );
        }
    }

    private void persistAll(
            SourceData s,
            InvoiceData i,
            List<MatchResult> matches,
            List<ExceptionCase> exceptions,
            List<Approval> approvals,
            List<Payment> payments,
            List<InvoiceStatusEvent> events) throws IOException {

        // Clear child/detail tables first.
        invoiceStatusEventRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        approvalRepository.deleteAllInBatch();
        exceptionCaseRepository.deleteAllInBatch();
        matchResultRepository.deleteAllInBatch();
        groundTruthRepository.deleteAllInBatch();
        invoiceLineRepository.deleteAllInBatch();
        invoiceRepository.deleteAllInBatch();
        goodsReceiptLineRepository.deleteAllInBatch();
        goodsReceiptRepository.deleteAllInBatch();
        purchaseOrderLineRepository.deleteAllInBatch();
        purchaseOrderRepository.deleteAllInBatch();
        vendorRepository.deleteAllInBatch();

        // Save normalized source/reference tables.
        vendorRepository.saveAll(
                s.vendors().stream().map(VendorEntity::from).toList()
        );

        purchaseOrderRepository.saveAll(
                s.purchaseOrders().stream()
                        .map(PurchaseOrderEntity::from).toList()
        );

        purchaseOrderLineRepository.saveAll(
                s.poLines().stream()
                        .map(PurchaseOrderLineEntity::from).toList()
        );

        goodsReceiptRepository.saveAll(
                s.receipts().stream()
                        .map(GoodsReceiptEntity::from).toList()
        );

        goodsReceiptLineRepository.saveAll(
                s.receiptLines().stream()
                        .map(GoodsReceiptLineEntity::from).toList()
        );

        // Save invoice headers and detail lines.
        invoiceRepository.saveAll(
                i.invoices().stream().map(InvoiceEntity::from).toList()
        );

        invoiceLineRepository.saveAll(
                i.invoiceLines().stream()
                        .map(InvoiceLineEntity::from).toList()
        );

        // Save labels, matching results, and workflow records.
        groundTruthRepository.saveAll(
                i.groundTruth().stream()
                        .map(GroundTruthEntity::from).toList()
        );

        matchResultRepository.saveAll(
                matches.stream().map(MatchResultEntity::from).toList()
        );

        exceptionCaseRepository.saveAll(
                exceptions.stream()
                        .map(ExceptionCaseEntity::from).toList()
        );

        approvalRepository.saveAll(
                approvals.stream().map(ApprovalEntity::from).toList()
        );

        paymentRepository.saveAll(
                payments.stream().map(PaymentEntity::from).toList()
        );

        invoiceStatusEventRepository.saveAll(
                events.stream()
                        .map(InvoiceStatusEventEntity::from).toList()
        );

        // Refresh generated JSON snapshots.
        generatedRecordRepository.deleteAllInBatch();

        List<GeneratedRecordEntity> records = new ArrayList<>();

        addRecords(records, "vendor", s.vendors());
        addRecords(records, "purchase_order", s.purchaseOrders());
        addRecords(records, "purchase_order_line", s.poLines());
        addRecords(records, "goods_receipt", s.receipts());
        addRecords(records, "goods_receipt_line", s.receiptLines());
        addRecords(records, "invoice", i.invoices());
        addRecords(records, "invoice_line", i.invoiceLines());
        addRecords(records, "ground_truth", i.groundTruth());
        addRecords(records, "match_result", matches);
        addRecords(records, "exception_case", exceptions);
        addRecords(records, "approval", approvals);
        addRecords(records, "payment", payments);
        addRecords(records, "invoice_status_event", events);

        generatedRecordRepository.saveAll(records);
    }

    private void addRecords(
            List<GeneratedRecordEntity> target,
            String type,
            List<?> values) throws IOException {

        for (Object value : values) {
            String id = recordId(value);

            target.add(new GeneratedRecordEntity(
                    type,
                    id,
                    mapper.writeValueAsString(value)
            ));
        }
    }

    private String recordId(Object value) {
        try {
            var components = value.getClass().getRecordComponents();

            Object id = components[0].getAccessor().invoke(value);

            return String.valueOf(id);

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Dataset row is not a supported record: "
                            + value.getClass(),
                    e
            );
        }
    }

    private void writeValidationReport(
            ValidationReport r,
            Path path) throws IOException {

        StringBuilder md = new StringBuilder(
                "# Validation Summary\n\n"
        )
                .append("- **Result:** ")
                .append(r.passed() ? "PASS" : "FAIL")
                .append("\n")
                .append("- **Checks run:** ")
                .append(r.checksRun())
                .append("\n\n## Errors\n");

        if (r.errors().isEmpty()) {
            md.append("- None\n");
        } else {
            r.errors().forEach(
                    x -> md.append("- ").append(x).append('\n')
            );
        }

        md.append("\n## Warnings\n");

        if (r.warnings().isEmpty()) {
            md.append("- None\n");
        } else {
            r.warnings().forEach(
                    x -> md.append("- ").append(x).append('\n')
            );
        }

        Files.writeString(
                path,
                md.toString(),
                StandardCharsets.UTF_8
        );
    }
}