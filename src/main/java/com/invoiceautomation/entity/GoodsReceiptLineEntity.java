package com.invoiceautomation.entity;

import com.invoiceautomation.enums.ReceiptType;
import com.invoiceautomation.model.GoodsReceiptLine;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Table(name = "goods_receipt_lines")
public class GoodsReceiptLineEntity {
    @Id @Column(name = "grn_line_id", nullable = false, length = 120) private String grnLineId;
    @Column(name = "grn_id", nullable = false, length = 100) private String grnId;
    @Column(name = "po_line_id", nullable = false, length = 100) private String poLineId;
    @Column(name = "received_qty", nullable = false) private int receivedQty;
    @Enumerated(EnumType.STRING) @Column(name = "receipt_type", length = 30) private ReceiptType receiptType;
    protected GoodsReceiptLineEntity() {}
    public static GoodsReceiptLineEntity from(GoodsReceiptLine g) { GoodsReceiptLineEntity e=new GoodsReceiptLineEntity(); e.grnLineId=g.grnLineId(); e.grnId=g.grnId(); e.poLineId=g.poLineId(); e.receivedQty=g.receivedQty(); e.receiptType=g.receiptType(); return e; }
}
