package com.invoiceautomation.model;

import com.invoiceautomation.enums.LifecycleStatus;

import java.time.LocalDate;

public record InvoiceStatusEvent(
        String eventId, String invoiceId, LifecycleStatus fromStatus, LifecycleStatus toStatus,
        LocalDate eventDate, String reason) { }