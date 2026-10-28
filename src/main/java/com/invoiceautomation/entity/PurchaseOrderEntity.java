package com.invoiceautomation.entity;

import com.invoiceautomation.enums.PoStatus;
import com.invoiceautomation.model.PurchaseOrder;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Table(name = "purchase_orders")
public class PurchaseOrderEntity {
    @Id @Column(name = "po_id", nullable = false, length = 80) private String poId;
    @Column(name = "vendor_id", nullable = false, length = 80) private String vendorId;
    @Column(name = "po_date") private LocalDate poDate;
    @Column(name = "expected_delivery_date") private LocalDate expectedDeliveryDate;
    @Enumerated(EnumType.STRING) @Column(length = 40) private PoStatus status;
    @Column(length = 10) private String currency;
    @Column(name = "total_amount", precision = 19, scale = 2) private BigDecimal totalAmount;
    protected PurchaseOrderEntity() {}
    public static PurchaseOrderEntity from(PurchaseOrder p) { PurchaseOrderEntity e=new PurchaseOrderEntity(); e.poId=p.poId(); e.vendorId=p.vendorId(); e.poDate=p.poDate(); e.expectedDeliveryDate=p.expectedDeliveryDate(); e.status=p.status(); e.currency=p.currency(); e.totalAmount=p.totalAmount(); return e; }
}
