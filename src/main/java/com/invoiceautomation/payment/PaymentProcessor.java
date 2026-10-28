package com.invoiceautomation.payment;

import com.invoiceautomation.enums.PaymentStatus;
import com.invoiceautomation.model.Payment;
import com.invoiceautomation.util.IdFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentProcessor {
    private final IdFactory ids = new IdFactory();

    public Payment createPayment(String invoiceId, BigDecimal amount, LocalDate scheduledDate) {
        return new Payment(ids.nextPaymentId(), invoiceId, PaymentStatus.PAYMENT_READY, scheduledDate, null, amount);
    }

    public Payment schedulePayment(Payment payment) {
        if (payment.paymentStatus() == PaymentStatus.PAID) throw new IllegalStateException("Payment has already been completed: " + payment.paymentId());
        return new Payment(payment.paymentId(), payment.invoiceId(), PaymentStatus.SCHEDULED, payment.scheduledDate(), null, payment.amount());
    }

    public Payment markAsPaid(Payment payment, LocalDate paidDate) {
        if (payment.paymentStatus() != PaymentStatus.SCHEDULED) throw new IllegalStateException("Only scheduled payments can be marked as paid: " + payment.paymentId());
        if (paidDate == null) throw new IllegalArgumentException("Paid date cannot be null");
        return new Payment(payment.paymentId(), payment.invoiceId(), PaymentStatus.PAID, payment.scheduledDate(), paidDate, payment.amount());
    }

    public List<Payment> createPaymentsForApprovedInvoices(List<String> invoiceIds, List<BigDecimal> amounts, LocalDate scheduledDate) {
        if (invoiceIds.size() != amounts.size()) throw new IllegalArgumentException("Invoice IDs and amounts must have the same size");
        List<Payment> payments = new ArrayList<>();
        for (int i = 0; i < invoiceIds.size(); i++) payments.add(createPayment(invoiceIds.get(i), amounts.get(i), scheduledDate));
        return payments;
    }
}
