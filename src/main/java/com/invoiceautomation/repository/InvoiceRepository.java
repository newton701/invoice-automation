package com.invoiceautomation.repository;


import com.invoiceautomation.entity.InvoiceEntity;
import com.invoiceautomation.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {
    List<InvoiceEntity> findByVendorId(String vendorId);
    List<InvoiceEntity> findByMatchStatus(MatchStatus matchStatus);
}
