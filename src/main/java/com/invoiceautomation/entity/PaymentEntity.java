package com.invoiceautomation.entity;

import com.invoiceautomation.enums.PaymentStatus;
import com.invoiceautomation.model.Payment;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Table(name = "payments")
public class PaymentEntity {
    @Id @Column(name = "payment_id", nullable = false, length = 120) private String paymentId;
    @Column(name = "invoice_id", nullable = false, length = 80) private String invoiceId;
    @Enumerated(EnumType.STRING) @Column(name = "payment_status", length = 40) private PaymentStatus paymentStatus;
    @Column(name = "scheduled_date") private LocalDate scheduledDate;
    @Column(name = "paid_date") private LocalDate paidDate;
    @Column(precision = 19, scale = 2) private BigDecimal amount;
    protected PaymentEntity() {}
    public static PaymentEntity from(Payment p) { PaymentEntity e=new PaymentEntity(); e.paymentId=p.paymentId(); e.invoiceId=p.invoiceId(); e.paymentStatus=p.paymentStatus(); e.scheduledDate=p.scheduledDate(); e.paidDate=p.paidDate(); e.amount=p.amount(); return e; }
}
