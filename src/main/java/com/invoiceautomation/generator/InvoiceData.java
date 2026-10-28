
package com.invoiceautomation.generator;

import com.invoiceautomation.model.GroundTruth;
import com.invoiceautomation.model.Invoice;
import com.invoiceautomation.model.InvoiceLine;

import java.util.List;

/**
 * Holds generated invoice records and their separate evaluation labels.
 */
public record InvoiceData(
        List<Invoice> invoices,
        List<InvoiceLine> invoiceLines,
        List<GroundTruth> groundTruth
) {
    public InvoiceData {
        invoices = List.copyOf(invoices);
        invoiceLines = List.copyOf(invoiceLines);
        groundTruth = List.copyOf(groundTruth);
    }
}
