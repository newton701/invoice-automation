
package com.invoiceautomation.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.invoiceautomation.model.GroundTruth;
import com.invoiceautomation.model.Invoice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JsonExporter {

    private final ObjectMapper objectMapper;

    public JsonExporter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(
                SerializationFeature.INDENT_OUTPUT
        );
    }

    public void exportInvoices(
            List<Invoice> invoices,
            Path outputPath) throws IOException {

        writeJson(invoices, outputPath);
    }

    public void exportGroundTruth(
            List<GroundTruth> groundTruthRecords,
            Path outputPath) throws IOException {

        writeJson(groundTruthRecords, outputPath);
    }

    private void writeJson(
            Object data,
            Path outputPath) throws IOException {

        Path parent = outputPath.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        objectMapper.writeValue(
                outputPath.toFile(),
                data
        );
    }
}
