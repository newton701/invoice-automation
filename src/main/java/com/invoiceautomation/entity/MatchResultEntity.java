package com.invoiceautomation.entity;

import com.invoiceautomation.enums.MatchStatus;
import com.invoiceautomation.model.MatchResult;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Table(name = "match_results")
public class MatchResultEntity {
    @Id @Column(name = "match_id", nullable = false, length = 120) private String matchId;
    @Column(name = "invoice_line_id", nullable = false, length = 120) private String invoiceLineId;
    @Column(name = "po_line_id", length = 100) private String poLineId;
    @Column(name = "grn_line_id", length = 120) private String grnLineId;
    @Column(name = "price_variance_pct", precision = 19, scale = 6) private BigDecimal priceVariancePct;
    @Column(name = "qty_variance", nullable = false) private int qtyVariance;
    @Column(name = "tax_variance", precision = 19, scale = 2) private BigDecimal taxVariance;
    @Enumerated(EnumType.STRING) @Column(name = "line_match_status", length = 40) private MatchStatus lineMatchStatus;
    protected MatchResultEntity() {}
    public static MatchResultEntity from(MatchResult m) { MatchResultEntity e=new MatchResultEntity(); e.matchId=m.matchId(); e.invoiceLineId=m.invoiceLineId(); e.poLineId=m.poLineId(); e.grnLineId=m.grnLineId(); e.priceVariancePct=m.priceVariancePct(); e.qtyVariance=m.qtyVariance(); e.taxVariance=m.taxVariance(); e.lineMatchStatus=m.lineMatchStatus(); return e; }
}
