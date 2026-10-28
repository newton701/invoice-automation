package com.invoiceautomation.util;

/**
 * Single source of all entity IDs. Counters are per-instance and start at 1,
 * so the same call sequence always yields the same IDs (needed for reproducibility).
 */
public final class IdFactory {

    private static final int YEAR = 2026;

    private int vendor, po, grn, invoice, approval, payment, event, match, exceptionCase;

    public String nextVendorId()   { return String.format("VND-%05d", ++vendor); }
    public String nextPoId()       { return String.format("PO-%d-%06d", YEAR, ++po); }
    public String nextGrnId()      { return String.format("GRN-%d-%06d", YEAR, ++grn); }
    public String nextApprovalId() { return String.format("APR-%d-%06d", YEAR, ++approval); }
    public String nextPaymentId()  { return String.format("PAY-%d-%06d", YEAR, ++payment); }
    public String nextEventId()    { return String.format("EVT-%07d", ++event); }
    public String nextMatchId()    { return String.format("MTH-%07d", ++match); }
    public String nextCaseId()     { return String.format("CASE-%06d", ++exceptionCase); }

    /** Invoice ID embeds the vendor code but uses one global counter. */
    public String nextInvoiceId(String vendorId) {
        return String.format("INV-%s-%d-%06d", vendorCode(vendorId), YEAR, ++invoice);
    }

    // ---- child IDs are derived from the parent ID, never from a counter ----

    public static String poLineId(String poId, int lineNumber)           { return String.format("%s-L%02d", poId, lineNumber); }
    public static String grnLineId(String grnId, int lineNumber)         { return String.format("%s-%02d", grnId, lineNumber); }
    public static String invoiceLineId(String invoiceId, int lineNumber) { return String.format("INVL-%s-%02d", invoiceId, lineNumber); }

    /** "VND-00042" -> "00042" (the data dictionary says vendor_code equals this). */
    public static String vendorCode(String vendorId) {
        return vendorId.substring(vendorId.indexOf('-') + 1);
    }
}