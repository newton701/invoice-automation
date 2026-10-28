package com.invoiceautomation.repository;


import com.invoiceautomation.entity.InvoiceLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLineEntity, String> {
}
