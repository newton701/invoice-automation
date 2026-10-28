package com.invoiceautomation.repository;


import com.invoiceautomation.entity.InvoiceStatusEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceStatusEventRepository extends JpaRepository<InvoiceStatusEventEntity, String> {
}
