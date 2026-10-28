package com.invoiceautomation.model;

import com.invoiceautomation.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Payment(
        String paymentId, String invoiceId, PaymentStatus paymentStatus,
        LocalDate scheduledDate, LocalDate paidDate, BigDecimal amount) {

    public Payment {
        // data dictionary: paid_date is required if and only if status = PAID
        if ((paymentStatus == PaymentStatus.PAID) != (paidDate != null)) {
            throw new IllegalArgumentException(paymentId + ": paid_date must be set only when PAID");
        }
        if (paidDate != null && scheduledDate != null && paidDate.isBefore(scheduledDate)) {
            throw new IllegalArgumentException(paymentId + ": paid_date before scheduled_date");
        }
    }
}