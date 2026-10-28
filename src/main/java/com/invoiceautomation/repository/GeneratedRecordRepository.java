package com.invoiceautomation.repository;


import com.invoiceautomation.entity.GeneratedRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GeneratedRecordRepository extends JpaRepository<GeneratedRecordEntity, String> {
    List<GeneratedRecordEntity> findByRecordType(String recordType);
}
