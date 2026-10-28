package com.invoiceautomation.model;

import com.invoiceautomation.enums.ApprovalDecision;
import com.invoiceautomation.enums.ApproverRole;

import java.time.LocalDate;

public record Approval(
        String approvalId, String invoiceId, ApproverRole approverRole,
        ApprovalDecision decision, LocalDate decisionDate) { }