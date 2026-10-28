package com.invoiceautomation.entity;

import com.invoiceautomation.enums.LifecycleStatus;
import com.invoiceautomation.enums.MatchStatus;
import com.invoiceautomation.enums.SourceChannel;
import com.invoiceautomation.model.Invoice;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
public class InvoiceEntity {
    @Id @Column(name = "invoice_id", nullable = false, length = 80)
    private String invoiceId;
    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;
    @Column(name = "vendor_id", nullable = false, length = 80)
    private String vendorId;
    @Column(name = "po_id", length = 80)
    private String poId;
    @Column(name = "invoice_date") private LocalDate invoiceDate;
    @Column(name = "due_date") private LocalDate dueDate;
    @Column(precision = 14, scale = 2) private BigDecimal subtotal;
    @Column(name = "tax_total", precision = 14, scale = 2) private BigDecimal taxTotal;
    @Column(name = "grand_total", precision = 14, scale = 2) private BigDecimal grandTotal;
    @Enumerated(EnumType.STRING) @Column(name = "source_channel", length = 40) private SourceChannel sourceChannel;
    @Enumerated(EnumType.STRING) @Column(name = "lifecycle_status", length = 40) private LifecycleStatus lifecycleStatus;
    @Enumerated(EnumType.STRING) @Column(name = "match_status", length = 40) private MatchStatus matchStatus;

    protected InvoiceEntity() {}
    public static InvoiceEntity from(Invoice invoice) {
        InvoiceEntity entity = new InvoiceEntity();
        entity.invoiceId = invoice.invoiceId(); entity.invoiceNumber = invoice.invoiceNumber();
        entity.vendorId = invoice.vendorId(); entity.poId = invoice.poId();
        entity.invoiceDate = invoice.invoiceDate(); entity.dueDate = invoice.dueDate();
        entity.subtotal = invoice.subtotal(); entity.taxTotal = invoice.taxTotal(); entity.grandTotal = invoice.grandTotal();
        entity.sourceChannel = invoice.sourceChannel(); entity.lifecycleStatus = invoice.lifecycleStatus(); entity.matchStatus = invoice.matchStatus();
        return entity;
    }
    public String getInvoiceId() { return invoiceId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getVendorId() { return vendorId; }
    public String getPoId() { return poId; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTaxTotal() { return taxTotal; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public SourceChannel getSourceChannel() { return sourceChannel; }
    public LifecycleStatus getLifecycleStatus() { return lifecycleStatus; }
    public MatchStatus getMatchStatus() { return matchStatus; }
}
