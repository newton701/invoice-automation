package com.invoiceautomation.controller;


import com.invoiceautomation.entity.GeneratedRecordEntity;
import com.invoiceautomation.repository.GeneratedRecordRepository;
import com.invoiceautomation.repository.InvoiceRepository;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {
    private final GeneratedRecordRepository repository;
    private final InvoiceRepository invoiceRepository;
    public DatasetController(GeneratedRecordRepository repository, InvoiceRepository invoiceRepository) {
        this.repository = repository;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/counts")
    public Map<String, Long> counts() {
        Map<String, Long> result = repository.findAll().stream().collect(Collectors.groupingBy(GeneratedRecordEntity::getRecordType, LinkedHashMap::new, Collectors.counting()));
        result.put("invoice_headers", invoiceRepository.count());
        return result;
    }

    /** Returns stored JSON payload strings for a dataset type, e.g. vendor or match_result. */
    @GetMapping("/{recordType}")
    public List<String> records(@PathVariable String recordType) {
        return repository.findByRecordType(recordType).stream().map(GeneratedRecordEntity::getPayload).toList();
    }
}
