package com.invoiceautomation.entity;

import com.invoiceautomation.enums.TaxCode;
import com.invoiceautomation.model.PurchaseOrderLine;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Table(name = "purchase_order_lines")
public class PurchaseOrderLineEntity {
    @Id @Column(name = "po_line_id", nullable = false, length = 100) private String poLineId;
    @Column(name = "po_id", nullable = false, length = 80) private String poId;
    @Column(name = "line_number", nullable = false) private int lineNumber;
    @Column(length = 100) private String sku;
    @Column(length = 500) private String description;
    @Column(name = "ordered_qty", nullable = false) private int orderedQty;
    @Column(name = "unit_price", precision = 19, scale = 2) private BigDecimal unitPrice;
    @Enumerated(EnumType.STRING) @Column(length = 30) private TaxCode taxCode;
    @Column(name = "line_amount", precision = 19, scale = 2) private BigDecimal lineAmount;
    protected PurchaseOrderLineEntity() {}
    public static PurchaseOrderLineEntity from(PurchaseOrderLine p) { PurchaseOrderLineEntity e=new PurchaseOrderLineEntity(); e.poLineId=p.poLineId(); e.poId=p.poId(); e.lineNumber=p.lineNumber(); e.sku=p.sku(); e.description=p.description(); e.orderedQty=p.orderedQty(); e.unitPrice=p.unitPrice(); e.taxCode=p.taxCode(); e.lineAmount=p.lineAmount(); return e; }
}
