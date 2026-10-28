package com.invoiceautomation.model;

import com.invoiceautomation.enums.VendorStatus;

import java.time.LocalDate;

public record Vendor(
        String vendorId, String vendorCode, String vendorName, VendorStatus status,
        boolean poRequired, int paymentTermsDays, String taxRegistrationNo,
        LocalDate onboardedDate, LocalDate closedDate) {

    public Vendor {
        // data dictionary: closed_date is required if and only if status = CLOSED
        if ((status == VendorStatus.CLOSED) != (closedDate != null)) {
            throw new IllegalArgumentException(vendorId + ": closed_date must be set only when CLOSED");
        }
        if (closedDate != null && closedDate.isBefore(onboardedDate)) {
            throw new IllegalArgumentException(vendorId + ": closed_date before onboarded_date");
        }
    }
}