package com.invoiceautomation.workflow;

import com.invoiceautomation.enums.ApprovalDecision;
import com.invoiceautomation.enums.ApproverRole;
import com.invoiceautomation.model.Approval;
import com.invoiceautomation.util.IdFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ApprovalWorkflow {
    private final IdFactory ids = new IdFactory();

    public List<Approval> createApprovals(String invoiceId, ApprovalDecision processOwnerDecision,
                                          ApprovalDecision approverDecision) {
        return createApprovals(invoiceId, processOwnerDecision, approverDecision, LocalDate.now());
    }

    public List<Approval> createApprovals(String invoiceId, ApprovalDecision processOwnerDecision,
                                          ApprovalDecision approverDecision, LocalDate decisionDate) {
        List<Approval> approvals = new ArrayList<>();
        approvals.add(new Approval(ids.nextApprovalId(), invoiceId, ApproverRole.PROCESS_OWNER,
                processOwnerDecision, decisionDate));
        if (processOwnerDecision == ApprovalDecision.APPROVED) {
            approvals.add(new Approval(ids.nextApprovalId(), invoiceId, ApproverRole.APPROVER,
                    approverDecision, decisionDate));
        }
        return approvals;
    }

    public boolean isFullyApproved(List<Approval> approvals) {
        boolean owner = approvals.stream().anyMatch(a -> a.approverRole() == ApproverRole.PROCESS_OWNER && a.decision() == ApprovalDecision.APPROVED);
        boolean approver = approvals.stream().anyMatch(a -> a.approverRole() == ApproverRole.APPROVER && a.decision() == ApprovalDecision.APPROVED);
        return owner && approver;
    }
}
