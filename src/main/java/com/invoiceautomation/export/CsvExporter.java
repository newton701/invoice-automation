
package com.invoiceautomation.export;

import com.invoiceautomation.model.Invoice;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.invoiceautomation.model.GroundTruth;
import java.util.stream.Collectors;

public class CsvExporter {

    public void exportInvoices(
            List<Invoice> invoices,
            Path outputPath) throws IOException {

        Path parent = outputPath.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                outputPath,
                StandardCharsets.UTF_8)) {

            writer.write(
                    "invoice_id,invoice_number,vendor_id,po_id,"
                            + "invoice_date,due_date,subtotal,tax_total,"
                            + "grand_total,source_channel,lifecycle_status,match_status"
            );
            writer.newLine();

            for (Invoice invoice : invoices) {
                writer.write(String.join(",",
                        csv(invoice.invoiceId()),
                        csv(invoice.invoiceNumber()),
                        csv(invoice.vendorId()),
                        csv(invoice.poId()),
                        csv(String.valueOf(invoice.invoiceDate())),
                        csv(String.valueOf(invoice.dueDate())),
                        csv(String.valueOf(invoice.subtotal())),
                        csv(String.valueOf(invoice.taxTotal())),
                        csv(String.valueOf(invoice.grandTotal())),
                        csv(String.valueOf(invoice.sourceChannel())),
                        csv(String.valueOf(invoice.lifecycleStatus())),
                        csv(String.valueOf(invoice.matchStatus()))
                ));
                writer.newLine();
            }
        }
    }


    public void exportGroundTruth(
            List<GroundTruth> groundTruthRecords,
            Path outputPath) throws IOException {

        Path parent = outputPath.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                outputPath,
                StandardCharsets.UTF_8)) {

            writer.write(
                    "invoice_id,issue_type,edge_case_tags,"
                            + "duplicate_of_invoice_id,expected_match_status,"
                            + "expected_payment_ready"
            );
            writer.newLine();

            for (GroundTruth truth : groundTruthRecords) {
                String tags = truth.edgeCaseTags()
                        .stream()
                        .collect(Collectors.joining(";"));

                writer.write(String.join(",",
                        csv(truth.invoiceId()),
                        csv(String.valueOf(truth.issueType())),
                        csv(tags),
                        csv(truth.duplicateOfInvoiceId()),
                        csv(String.valueOf(truth.expectedMatchStatus())),
                        csv(String.valueOf(truth.expectedPaymentReady()))
                ));

                writer.newLine();
            }
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");

        if (escaped.contains(",")
                || escaped.contains("\"")
                || escaped.contains("\n")
                || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }

        return escaped;
    }
}
