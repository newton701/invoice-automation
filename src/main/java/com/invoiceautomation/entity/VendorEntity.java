package com.invoiceautomation.entity;

import com.invoiceautomation.enums.VendorStatus;
import com.invoiceautomation.model.Vendor;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Table(name = "vendors")
public class VendorEntity {
    @Id @Column(name = "vendor_id", nullable = false, length = 80) private String vendorId;
    @Column(name = "vendor_code", nullable = false, unique = true, length = 40) private String vendorCode;
    @Column(name = "vendor_name", nullable = false, length = 200) private String vendorName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private VendorStatus status;
    @Column(name = "po_required", nullable = false) private boolean poRequired;
    @Column(name = "payment_terms_days", nullable = false) private int paymentTermsDays;
    @Column(name = "tax_registration_no", length = 100) private String taxRegistrationNo;
    @Column(name = "onboarded_date") private LocalDate onboardedDate;
    @Column(name = "closed_date") private LocalDate closedDate;
    protected VendorEntity() {}
    public static VendorEntity from(Vendor v) { VendorEntity e=new VendorEntity(); e.vendorId=v.vendorId(); e.vendorCode=v.vendorCode(); e.vendorName=v.vendorName(); e.status=v.status(); e.poRequired=v.poRequired(); e.paymentTermsDays=v.paymentTermsDays(); e.taxRegistrationNo=v.taxRegistrationNo(); e.onboardedDate=v.onboardedDate(); e.closedDate=v.closedDate(); return e; }
}
