package com.invoiceautomation.entity;

import com.invoiceautomation.enums.GrnStatus;
import com.invoiceautomation.model.GoodsReceipt;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Table(name = "goods_receipts")
public class GoodsReceiptEntity {
    @Id @Column(name = "grn_id", nullable = false, length = 100) private String grnId;
    @Column(name = "po_id", nullable = false, length = 80) private String poId;
    @Column(name = "receipt_date") private LocalDate receiptDate;
    @Enumerated(EnumType.STRING) @Column(length = 30) private GrnStatus status;
    protected GoodsReceiptEntity() {}
    public static GoodsReceiptEntity from(GoodsReceipt g) { GoodsReceiptEntity e=new GoodsReceiptEntity(); e.grnId=g.grnId(); e.poId=g.poId(); e.receiptDate=g.receiptDate(); e.status=g.status(); return e; }
}
