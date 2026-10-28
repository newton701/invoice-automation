package com.invoiceautomation.entity;

import com.invoiceautomation.enums.TaxCode;
import com.invoiceautomation.model.InvoiceLine;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Table(name = "invoice_lines")
public class InvoiceLineEntity {
    @Id @Column(name = "invoice_line_id", nullable = false, length = 120) private String invoiceLineId;
    @Column(name = "invoice_id", nullable = false, length = 80) private String invoiceId;
    @Column(name = "line_number", nullable = false) private int lineNumber;
    @Column(name = "po_line_id", length = 100) private String poLineId;
    @Column(length = 100) private String sku;
    @Column(nullable = false) private int quantity;
    @Column(name = "unit_price", precision = 19, scale = 2) private BigDecimal unitPrice;
    @Enumerated(EnumType.STRING) @Column(length = 30) private TaxCode taxCode;
    @Column(name = "line_amount", precision = 19, scale = 2) private BigDecimal lineAmount;
    @Column(name = "tax_amount", precision = 19, scale = 2) private BigDecimal taxAmount;
    @Column(name = "line_total", precision = 19, scale = 2) private BigDecimal lineTotal;
    protected InvoiceLineEntity() {}
    public static InvoiceLineEntity from(InvoiceLine l) { InvoiceLineEntity e=new InvoiceLineEntity(); e.invoiceLineId=l.invoiceLineId(); e.invoiceId=l.invoiceId(); e.lineNumber=l.lineNumber(); e.poLineId=l.poLineId(); e.sku=l.sku(); e.quantity=l.quantity(); e.unitPrice=l.unitPrice(); e.taxCode=l.taxCode(); e.lineAmount=l.lineAmount(); e.taxAmount=l.taxAmount(); e.lineTotal=l.lineTotal(); return e; }
}
