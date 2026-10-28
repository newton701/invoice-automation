package com.invoiceautomation.entity;

import com.invoiceautomation.enums.ApprovalDecision;
import com.invoiceautomation.enums.ApproverRole;
import com.invoiceautomation.model.Approval;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Table(name = "approvals")
public class ApprovalEntity {
    @Id @Column(name = "approval_id", nullable = false, length = 120) private String approvalId;
    @Column(name = "invoice_id", nullable = false, length = 80) private String invoiceId;
    @Enumerated(EnumType.STRING) @Column(name = "approver_role", length = 40) private ApproverRole approverRole;
    @Enumerated(EnumType.STRING) @Column(length = 30) private ApprovalDecision decision;
    @Column(name = "decision_date") private LocalDate decisionDate;
    protected ApprovalEntity() {}
    public static ApprovalEntity from(Approval a) { ApprovalEntity e=new ApprovalEntity(); e.approvalId=a.approvalId(); e.invoiceId=a.invoiceId(); e.approverRole=a.approverRole(); e.decision=a.decision(); e.decisionDate=a.decisionDate(); return e; }
}
