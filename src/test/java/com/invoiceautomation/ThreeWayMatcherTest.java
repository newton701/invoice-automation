package com.invoiceautomation;

import com.invoiceautomation.config.GeneratorConfig;
import com.invoiceautomation.enums.MatchStatus;
import com.invoiceautomation.generator.*;
import com.invoiceautomation.matching.ThreeWayMatcher;
import com.invoiceautomation.model.*;
import com.invoiceautomation.util.IdFactory;
import com.invoiceautomation.validation.DuplicateInvoiceDetector;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class ThreeWayMatcherTest {
    @Test void matchesEveryInvoiceLineAndDetectsInjectedRuleFailures() {
        GeneratorConfig config = GeneratorConfig.defaults();
        SourceData source = new SourceDataGenerator(config, new IdFactory(), new Random(config.seed())).generate();
        InvoiceData data = new InvoiceGenerator(config, new IdFactory()).generate(source);
        List<MatchResult> matches = new ThreeWayMatcher().match(data.invoices(), data.invoiceLines(),
                source.purchaseOrders(), source.poLines(), source.receiptLines(), source.vendors());
        assertEquals(data.invoiceLines().size(), matches.size());
        Set<String> duplicateIds = new DuplicateInvoiceDetector().findDuplicateInvoiceIds(data.invoices());
        Map<String, String> invoiceByLine = data.invoiceLines().stream().collect(Collectors.toMap(InvoiceLine::invoiceLineId, InvoiceLine::invoiceId));
        Map<String, MatchStatus> actualByInvoice = new HashMap<>();

        for (MatchResult result : matches) {
            String invoiceId = invoiceByLine.get(result.invoiceLineId());

            if (duplicateIds.contains(invoiceId)) {
                actualByInvoice.put(invoiceId, MatchStatus.DUPLICATE);
                continue;
            }

            MatchStatus current = actualByInvoice.get(invoiceId);
            MatchStatus lineStatus = result.lineMatchStatus();

            if (current == null || current == MatchStatus.MATCHED) {
                actualByInvoice.put(invoiceId, lineStatus);
            }
        }
        Map<String, MatchStatus> expectedByInvoice = data.groundTruth().stream().collect(Collectors.toMap(GroundTruth::invoiceId, GroundTruth::expectedMatchStatus));
        for (Map.Entry<String, MatchStatus> entry : expectedByInvoice.entrySet()) {
            String invoiceId = entry.getKey();
            MatchStatus expected = entry.getValue();
            MatchStatus actual = actualByInvoice.get(invoiceId);

            if (expected != actual) {
                System.out.printf(
                        "MISMATCH | Invoice: %s | Expected: %s | Actual: %s%n",
                        invoiceId, expected, actual
                );
            }
        }

        assertEquals(expectedByInvoice, actualByInvoice);
        Map<MatchStatus, Long> statuses = matches.stream().collect(Collectors.groupingBy(MatchResult::lineMatchStatus, Collectors.counting()));
        assertTrue(statuses.getOrDefault(MatchStatus.PRICE_VARIANCE, 0L) >= 200);
        assertTrue(statuses.getOrDefault(MatchStatus.QUANTITY_MISMATCH, 0L) >= 160);
        assertTrue(statuses.getOrDefault(MatchStatus.MISSING_RECEIPT, 0L) >= 80);
        assertTrue(statuses.getOrDefault(MatchStatus.TAX_MISMATCH, 0L) >= 29);
        assertTrue(statuses.getOrDefault(MatchStatus.VENDOR_BLOCKED, 0L) >= 30);
        assertEquals(1L, statuses.getOrDefault(MatchStatus.DATE_ERROR, 0L).longValue());
        assertEquals(1L, statuses.getOrDefault(MatchStatus.ARITHMETIC_ERROR, 0L).longValue());
    }
}
