
package com.invoiceautomation.model;

import com.invoiceautomation.enums.CaseStatus;
import com.invoiceautomation.enums.MatchStatus;
import com.invoiceautomation.enums.RouteTarget;

import java.time.LocalDate;

public record ExceptionCase(
        String caseId,
        String invoiceId,
        MatchStatus exceptionType,
        RouteTarget routedTo,
        CaseStatus status,
        LocalDate openedDate
) {
}
