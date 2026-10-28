package com.invoiceautomation.model;

import com.invoiceautomation.enums.GrnStatus;

import java.time.LocalDate;

public record GoodsReceipt(String grnId, String poId, LocalDate receiptDate, GrnStatus status) {

}