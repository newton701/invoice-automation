package com.invoiceautomation;

import com.invoiceautomation.config.GeneratorConfig;
import com.invoiceautomation.generator.*;
import com.invoiceautomation.enums.IssueType;
import com.invoiceautomation.util.IdFactory;
import com.invoiceautomation.validation.DuplicateInvoiceDetector;
import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorReproducibilityTest {
    private record Generated(SourceData source, InvoiceData invoices) { }
    private Generated generate() {
        GeneratorConfig config = GeneratorConfig.defaults();
        SourceData source = new SourceDataGenerator(config, new IdFactory(), new Random(config.seed())).generate();
        InvoiceData invoices = new InvoiceGenerator(config, new IdFactory()).generate(source);
        return new Generated(source, invoices);
    }

    @Test void generatesRequiredVolumesAndScenarioMix() {
        Generated data = generate();
        assertEquals(300, data.source().vendors().size());
        assertEquals(1500, data.source().purchaseOrders().size());
        assertEquals(2200, data.source().poLines().size());
        assertEquals(1800, data.source().receipts().size());
        assertEquals(2000, data.invoices().invoices().size());
        assertEquals(2000, data.invoices().groundTruth().size());
        Map<IssueType, Long> counts = data.invoices().groundTruth().stream().collect(Collectors.groupingBy(g -> g.issueType(), Collectors.counting()));
        assertEquals(1400L, counts.get(IssueType.CLEAN).longValue());
        assertEquals(200L, counts.get(IssueType.PRICE_VARIANCE).longValue());
        assertEquals(160L, counts.get(IssueType.QUANTITY_MISMATCH).longValue());
        assertEquals(100L, counts.get(IssueType.DUPLICATE).longValue());
        assertEquals(80L, counts.get(IssueType.MISSING_RECEIPT).longValue());
        assertEquals(60L, counts.get(IssueType.TAX_VENDOR_ERROR).longValue());
    }

    @Test void sameSeedProducesIdenticalRecords() {
        assertEquals(generate(), generate());
    }

    @Test void duplicateDetectorFlagsOnlyResubmittedCopies() {
        Generated data = generate();
        assertEquals(100, new DuplicateInvoiceDetector().findDuplicateInvoiceIds(data.invoices().invoices()).size());
    }
}
