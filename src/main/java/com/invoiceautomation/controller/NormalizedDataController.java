package com.invoiceautomation.controller;


import com.invoiceautomation.repository.ApprovalRepository;
import com.invoiceautomation.repository.ExceptionCaseRepository;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/** Read-only endpoints backed directly by the normalized PostgreSQL tables. */
@RestController
@RequestMapping("/api")
public class NormalizedDataController {
    private final InvoiceRepository invoices;
    private final VendorRepository vendors;
    private final PurchaseOrderRepository purchaseOrders;
    private final PurchaseOrderLineRepository purchaseOrderLines;
    private final GoodsReceiptRepository goodsReceipts;
    private final GoodsReceiptLineRepository goodsReceiptLines;
    private final InvoiceLineRepository invoiceLines;
    private final GroundTruthRepository groundTruth;
    private final MatchResultRepository matchResults;
    private final ExceptionCaseRepository exceptionCases;
    private final ApprovalRepository approvals;
    private final PaymentRepository payments;
    private final InvoiceStatusEventRepository invoiceStatusEvents;

    public NormalizedDataController(InvoiceRepository invoices, VendorRepository vendors,
            PurchaseOrderRepository purchaseOrders,
            PurchaseOrderLineRepository purchaseOrderLines,
            GoodsReceiptRepository goodsReceipts,
            GoodsReceiptLineRepository goodsReceiptLines,
            InvoiceLineRepository invoiceLines,
            GroundTruthRepository groundTruth,
            MatchResultRepository matchResults,
            ExceptionCaseRepository exceptionCases,
            ApprovalRepository approvals,
            PaymentRepository payments,
            InvoiceStatusEventRepository invoiceStatusEvents) {
        this.invoices = invoices;
        this.vendors = vendors;
        this.purchaseOrders = purchaseOrders;
        this.purchaseOrderLines = purchaseOrderLines;
        this.goodsReceipts = goodsReceipts;
        this.goodsReceiptLines = goodsReceiptLines;
        this.invoiceLines = invoiceLines;
        this.groundTruth = groundTruth;
        this.matchResults = matchResults;
        this.exceptionCases = exceptionCases;
        this.approvals = approvals;
        this.payments = payments;
        this.invoiceStatusEvents = invoiceStatusEvents;
    }

    @GetMapping("/vendors") public List<?> getVendors() { return vendors.findAll(); }
    @GetMapping("/purchase-orders") public List<?> getPurchaseOrders() { return purchaseOrders.findAll(); }
    @GetMapping("/purchase-order-lines") public List<?> getPurchaseOrderLines() { return purchaseOrderLines.findAll(); }
    @GetMapping("/goods-receipts") public List<?> getGoodsReceipts() { return goodsReceipts.findAll(); }
    @GetMapping("/goods-receipt-lines") public List<?> getGoodsReceiptLines() { return goodsReceiptLines.findAll(); }
    @GetMapping("/invoice-lines") public List<?> getInvoiceLines() { return invoiceLines.findAll(); }
    @GetMapping("/ground-truth") public List<?> getGroundTruth() { return groundTruth.findAll(); }
    @GetMapping("/match-results") public List<?> getMatchResults() { return matchResults.findAll(); }
    @GetMapping("/exception-cases") public List<?> getExceptionCases() { return exceptionCases.findAll(); }
    @GetMapping("/approvals") public List<?> getApprovals() { return approvals.findAll(); }
    @GetMapping("/payments") public List<?> getPayments() { return payments.findAll(); }
    @GetMapping("/invoice-status-events") public List<?> getInvoiceStatusEvents() { return invoiceStatusEvents.findAll(); }

    @GetMapping("/normalized-counts")
    public Map<String, Long> normalizedCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("vendors", vendors.count());
        counts.put("purchase_orders", purchaseOrders.count());
        counts.put("purchase_order_lines", purchaseOrderLines.count());
        counts.put("goods_receipts", goodsReceipts.count());
        counts.put("goods_receipt_lines", goodsReceiptLines.count());
        counts.put("invoices", invoices.count());
        counts.put("invoice_lines", invoiceLines.count());
        counts.put("ground_truth", groundTruth.count());
        counts.put("match_results", matchResults.count());
        counts.put("exception_cases", exceptionCases.count());
        counts.put("approvals", approvals.count());
        counts.put("payments", payments.count());
        counts.put("invoice_status_events", invoiceStatusEvents.count());
        return counts;
    }
}
