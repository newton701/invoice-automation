package com.invoiceautomation.model;

import com.invoiceautomation.enums.ReceiptType;

public record GoodsReceiptLine(
        String grnLineId, String grnId, String poLineId, int receivedQty, ReceiptType receiptType) {

    /** PARTIAL if cumulative < ordered, FULL if equal, OVER if greater (data dictionary). */
    public static ReceiptType classify(int cumulativeReceivedQty, int orderedQty) {
        return Integer.compare(cumulativeReceivedQty, orderedQty) < 0 ? ReceiptType.PARTIAL
                : cumulativeReceivedQty == orderedQty ? ReceiptType.FULL : ReceiptType.OVER;
    }
}