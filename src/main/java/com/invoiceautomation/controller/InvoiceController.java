package com.invoiceautomation.controller;


import com.invoiceautomation.entity.InvoiceEntity;
import com.invoiceautomation.repository.InvoiceRepository;
import com.invoiceautomation.enums.MatchStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceRepository repository;
    public InvoiceController(InvoiceRepository repository) { this.repository = repository; }

    @GetMapping public List<InvoiceEntity> getAllInvoices() { return repository.findAll(); }
    @GetMapping("/count") public long countInvoices() { return repository.count(); }
    @GetMapping("/vendor/{vendorId}") public List<InvoiceEntity> getByVendor(@PathVariable String vendorId) { return repository.findByVendorId(vendorId); }
    @GetMapping("/match-status/{status}") public List<InvoiceEntity> getByMatchStatus(@PathVariable MatchStatus status) { return repository.findByMatchStatus(status); }
    @GetMapping("/{invoiceId}") public ResponseEntity<InvoiceEntity> getInvoice(@PathVariable String invoiceId) {
        return repository.findById(invoiceId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
