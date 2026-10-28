package com.invoiceautomation.model;

import com.invoiceautomation.enums.IssueType;
import com.invoiceautomation.enums.MatchStatus;

import java.util.List;

public record GroundTruth(
        String invoiceId, IssueType issueType, List<String> edgeCaseTags,
        String duplicateOfInvoiceId, MatchStatus expectedMatchStatus, boolean expectedPaymentReady) {

    public GroundTruth {
        // data dictionary: duplicate_of_invoice_id is null unless issue_type = DUPLICATE
        if ((issueType == IssueType.DUPLICATE) != (duplicateOfInvoiceId != null)) {
            throw new IllegalArgumentException(invoiceId + ": duplicate_of must be set only for DUPLICATE");
        }
        edgeCaseTags = List.copyOf(edgeCaseTags);
    }
}