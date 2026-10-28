package com.invoiceautomation.entity;

import com.invoiceautomation.enums.CaseStatus;
import com.invoiceautomation.enums.MatchStatus;
import com.invoiceautomation.enums.RouteTarget;
import com.invoiceautomation.model.ExceptionCase;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Table(name = "exception_cases")
public class ExceptionCaseEntity {
    @Id @Column(name = "case_id", nullable = false, length = 120) private String caseId;
    @Column(name = "invoice_id", nullable = false, length = 80) private String invoiceId;
    @Enumerated(EnumType.STRING) @Column(name = "exception_type", length = 40) private MatchStatus exceptionType;
    @Enumerated(EnumType.STRING) @Column(name = "routed_to", length = 50) private RouteTarget routedTo;
    @Enumerated(EnumType.STRING) @Column(length = 30) private CaseStatus status;
    @Column(name = "opened_date") private LocalDate openedDate;
    protected ExceptionCaseEntity() {}
    public static ExceptionCaseEntity from(ExceptionCase c) { ExceptionCaseEntity e=new ExceptionCaseEntity(); e.caseId=c.caseId(); e.invoiceId=c.invoiceId(); e.exceptionType=c.exceptionType(); e.routedTo=c.routedTo(); e.status=c.status(); e.openedDate=c.openedDate(); return e; }
}
