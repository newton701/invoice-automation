package com.invoiceautomation.entity;

import com.invoiceautomation.enums.LifecycleStatus;
import com.invoiceautomation.model.InvoiceStatusEvent;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Table(name = "invoice_status_events")
public class InvoiceStatusEventEntity {
    @Id @Column(name = "event_id", nullable = false, length = 120) private String eventId;
    @Column(name = "invoice_id", nullable = false, length = 80) private String invoiceId;
    @Enumerated(EnumType.STRING) @Column(name = "from_status", length = 40) private LifecycleStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", length = 40) private LifecycleStatus toStatus;
    @Column(name = "event_date") private LocalDate eventDate;
    @Column(length = 1000) private String reason;
    protected InvoiceStatusEventEntity() {}
    public static InvoiceStatusEventEntity from(InvoiceStatusEvent e0) { InvoiceStatusEventEntity e=new InvoiceStatusEventEntity(); e.eventId=e0.eventId(); e.invoiceId=e0.invoiceId(); e.fromStatus=e0.fromStatus(); e.toStatus=e0.toStatus(); e.eventDate=e0.eventDate(); e.reason=e0.reason(); return e; }
}
