package com.invoiceautomation.repository;


import com.invoiceautomation.entity.PurchaseOrderLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLineEntity, String> {
}
