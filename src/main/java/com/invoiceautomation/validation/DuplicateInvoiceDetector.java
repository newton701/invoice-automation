
package com.invoiceautomation.validation;

import com.invoiceautomation.model.Invoice;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DuplicateInvoiceDetector {

    /**
     * Detects duplicate invoice submissions using vendor ID
     * and normalized invoice number.
     *
     * The first submission is preserved.
     * Subsequent duplicate invoice IDs are returned.
     */
    public Set<String> findDuplicateInvoiceIds(List<Invoice> invoices) {

        Set<String> seenInvoiceKeys = new HashSet<>();
        Set<String> duplicateInvoiceIds = new HashSet<>();

        if (invoices == null || invoices.isEmpty()) {
            return duplicateInvoiceIds;
        }

        for (Invoice invoice : invoices) {

            if (invoice == null
                    || invoice.vendorId() == null
                    || invoice.invoiceNumber() == null
                    || invoice.invoiceId() == null) {
                continue;
            }

            String vendorId = invoice.vendorId()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            String invoiceNumber = invoice.invoiceNumber()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (vendorId.isEmpty() || invoiceNumber.isEmpty()) {
                continue;
            }

            String duplicateKey = vendorId + "|" + invoiceNumber;

            if (!seenInvoiceKeys.add(duplicateKey)) {
                duplicateInvoiceIds.add(invoice.invoiceId());
            }
        }

        return duplicateInvoiceIds;
    }
}
