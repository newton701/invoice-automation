package com.invoiceautomation.service;

import com.invoiceautomation.validation.ValidationReport;
import java.util.Map;

public record PipelineSummary(
        boolean success,
        Map<String, Integer> recordCounts,
        Map<String, Integer> scenarioCounts,
        Map<String, Integer> matchCounts,
        int exceptionCount,
        int approvalCount,
        int paymentReadyCount,
        String datasetSha256,
        String outputDirectory,
        ValidationReport validation) { }
