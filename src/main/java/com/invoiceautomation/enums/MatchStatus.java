package com.invoiceautomation.enums;

public enum MatchStatus {
    MATCHED,
    PRICE_VARIANCE,
    QUANTITY_MISMATCH,
    MISSING_RECEIPT,
    TAX_MISMATCH,
    VENDOR_BLOCKED,
    DUPLICATE,
    ARITHMETIC_ERROR,
    DATE_ERROR,
    NO_PURCHASE_ORDER
}