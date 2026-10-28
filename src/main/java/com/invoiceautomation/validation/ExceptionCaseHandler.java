package com.invoiceautomation.validation;

import com.invoiceautomation.enums.CaseStatus;
import com.invoiceautomation.enums.MatchStatus;
import com.invoiceautomation.enums.RouteTarget;
import com.invoiceautomation.model.ExceptionCase;
import com.invoiceautomation.model.Invoice;
import com.invoiceautomation.model.MatchResult;
import com.invoiceautomation.util.IdFactory;
import java.time.LocalDate;
import java.util.*;

public class ExceptionCaseHandler {
    private final IdFactory ids = new IdFactory();

    public List<ExceptionCase> createExceptionCases(List<Invoice> invoices, List<MatchResult> matchResults) {
        List<ExceptionCase> exceptions = new ArrayList<>();
        Map<String, Invoice> invoicesById = new HashMap<>();
        for (Invoice invoice : invoices) invoicesById.put(invoice.invoiceId(), invoice);
        for (MatchResult result : matchResults) {
            String invoiceId = extractInvoiceId(result.invoiceLineId());
            if (invoiceId == null || !invoicesById.containsKey(invoiceId)) {
                continue;
            }
            MatchStatus status = result.lineMatchStatus();
            if (status == MatchStatus.MATCHED) continue;
            exceptions.add(new ExceptionCase(ids.nextCaseId(), invoiceId, status, determineRoute(status), CaseStatus.OPEN, LocalDate.of(2026, 10, 22)));
        }
        return exceptions;
    }

    private String extractInvoiceId(String invoiceLineId) {
        if (invoiceLineId == null || !invoiceLineId.startsWith("INVL-")) return null;
        int finalSeparator = invoiceLineId.lastIndexOf('-');
        if (finalSeparator <= 5) return null;
        return invoiceLineId.substring(5, finalSeparator);
    }

    private RouteTarget determineRoute(MatchStatus status) {
        return switch (status) {
            case PRICE_VARIANCE, TAX_MISMATCH, ARITHMETIC_ERROR, DATE_ERROR ->
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
}
