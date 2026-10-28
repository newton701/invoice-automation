package com.invoiceautomation.repository;


import com.invoiceautomation.entity.MatchResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResultEntity, String> {
}
