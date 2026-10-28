package com.invoiceautomation.entity;

import com.invoiceautomation.enums.IssueType;
import com.invoiceautomation.enums.MatchStatus;
import com.invoiceautomation.model.GroundTruth;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import java.util.List;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Table(name = "ground_truth")
public class GroundTruthEntity {
    @Id @Column(name = "invoice_id", nullable = false, length = 80) private String invoiceId;
    @Enumerated(EnumType.STRING) @Column(name = "issue_type", nullable = false, length = 40) private IssueType issueType;
    @Convert(converter = StringListConverter.class) @Column(name = "edge_case_tags", columnDefinition = "text") private List<String> edgeCaseTags;
    @Column(name = "duplicate_of_invoice_id", length = 80) private String duplicateOfInvoiceId;
    @Enumerated(EnumType.STRING) @Column(name = "expected_match_status", length = 40) private MatchStatus expectedMatchStatus;
    @Column(name = "expected_payment_ready", nullable = false) private boolean expectedPaymentReady;
    protected GroundTruthEntity() {}
    public static GroundTruthEntity from(GroundTruth g) { GroundTruthEntity e=new GroundTruthEntity(); e.invoiceId=g.invoiceId(); e.issueType=g.issueType(); e.edgeCaseTags=g.edgeCaseTags(); e.duplicateOfInvoiceId=g.duplicateOfInvoiceId(); e.expectedMatchStatus=g.expectedMatchStatus(); e.expectedPaymentReady=g.expectedPaymentReady(); return e; }
}
