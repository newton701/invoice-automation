package com.invoiceautomation.generator;

import com.invoiceautomation.model.GoodsReceipt;
import com.invoiceautomation.model.GoodsReceiptLine;
import com.invoiceautomation.model.PurchaseOrder;
import com.invoiceautomation.model.PurchaseOrderLine;
import com.invoiceautomation.model.Vendor;

import java.util.List;

/** The "source of truth" documents that invoices are later generated from. */
public record SourceData(
        List<Vendor> vendors,
        List<PurchaseOrder> purchaseOrders,
        List<PurchaseOrderLine> poLines,
        List<GoodsReceipt> receipts,
        List<GoodsReceiptLine> receiptLines) { }