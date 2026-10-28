package com.invoiceautomation.repository;


import com.invoiceautomation.entity.GoodsReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceiptEntity, String> {
}
