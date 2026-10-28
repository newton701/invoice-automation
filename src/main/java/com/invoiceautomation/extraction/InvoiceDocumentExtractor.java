package com.invoiceautomation.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.invoiceautomation.model.Invoice;
import com.invoiceautomation.model.InvoiceLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates deterministic text invoice documents and extracts key/value fields from them.
 */
public class InvoiceDocumentExtractor {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Creates a document for an invoice containing one line.
     * Preserved for backward compatibility.
     */
    public Map<String, Object> createDocument(
            Invoice invoice,
            InvoiceLine line) {

        Map<String, Object> document = new LinkedHashMap<>();

        document.put("invoice_id", invoice.invoiceId());
        document.put("invoice_number", invoice.invoiceNumber());
        document.put("vendor_id", invoice.vendorId());
        document.put("po_id", invoice.poId());
        document.put("invoice_date", invoice.invoiceDate());
        document.put("due_date", invoice.dueDate());
        document.put("subtotal", invoice.subtotal());
        document.put("tax_total", invoice.taxTotal());
        document.put("grand_total", invoice.grandTotal());

        document.put("line_sku", line.sku());
        document.put("line_quantity", line.quantity());
        document.put("line_unit_price", line.unitPrice());
        document.put("line_amount", line.lineAmount());
        document.put("line_tax_amount", line.taxAmount());
        document.put("line_total", line.lineTotal());

        return document;
    }

    /**
     * Creates a document for an invoice containing multiple lines.
     */
    public Map<String, Object> createDocument(
            Invoice invoice,
            List<InvoiceLine> lines) {

        Map<String, Object> document = new LinkedHashMap<>();

        document.put("invoice_id", invoice.invoiceId());
        document.put("invoice_number", invoice.invoiceNumber());
        document.put("vendor_id", invoice.vendorId());
        document.put("po_id", invoice.poId());
        document.put("invoice_date", invoice.invoiceDate());
        document.put("due_date", invoice.dueDate());
        document.put("subtotal", invoice.subtotal());
        document.put("tax_total", invoice.taxTotal());
        document.put("grand_total", invoice.grandTotal());

        List<Map<String, Object>> lineDocuments = lines.stream()
                .map(line -> {
                    Map<String, Object> lineDocument =
                            new LinkedHashMap<>();

                    lineDocument.put(
                            "invoice_line_id",
                            line.invoiceLineId()
                    );
                    lineDocument.put("line_number", line.lineNumber());
                    lineDocument.put("po_line_id", line.poLineId());
                    lineDocument.put("sku", line.sku());
                    lineDocument.put("quantity", line.quantity());
                    lineDocument.put("unit_price", line.unitPrice());
                    lineDocument.put("tax_code", line.taxCode());
                    lineDocument.put("line_amount", line.lineAmount());
                    lineDocument.put("tax_amount", line.taxAmount());
                    lineDocument.put("line_total", line.lineTotal());

                    return lineDocument;
                })
                .toList();

        document.put("lines", lineDocuments);

        return document;
    }

    public Map<String, Object> extract(Path documentPath)
            throws IOException {

        return extractJson(
                Files.readString(
                        documentPath,
                        StandardCharsets.UTF_8
                )
        );
    }

    public Map<String, Object> extractJson(String json)
            throws IOException {

        return mapper.readValue(
                json,
                mapper.getTypeFactory().constructMapType(
                        LinkedHashMap.class,
                        String.class,
                        Object.class
                )
        );
    }

    public String toJson(Map<String, Object> document)
            throws IOException {

        return mapper.writeValueAsString(document);
    }

    public void writeDocument(
            Invoice invoice,
            InvoiceLine line,
            Path path) throws IOException {

        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(path.toFile(), createDocument(invoice, line));
    }

    /**
     * Writes a document for an invoice containing multiple lines.
     */
    public void writeDocument(
            Invoice invoice,
            List<InvoiceLine> lines,
            Path path) throws IOException {

        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(path.toFile(), createDocument(invoice, lines));
    }
}