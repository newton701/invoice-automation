package com.invoiceautomation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Stores a lossless JSON snapshot for each generated dataset record. Normalized SQL seed is exported separately. */
@Entity
@Table(name = "generated_records", indexes = {
        @Index(name = "idx_generated_record_type", columnList = "record_type"),
        @Index(name = "idx_generated_record_id", columnList = "record_id")
})
public class GeneratedRecordEntity {
    @Id
    @Column(name = "storage_key", nullable = false, length = 300)
    private String storageKey;
    @Column(name = "record_type", nullable = false, length = 80)
    private String recordType;
    @Column(name = "record_id", nullable = false, length = 180)
    private String recordId;
    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    protected GeneratedRecordEntity() { }

    public GeneratedRecordEntity(String recordType, String recordId, String payload) {
        this.storageKey = recordType + "::" + recordId;
        this.recordType = recordType;
        this.recordId = recordId;
        this.payload = payload;
    }

    public String getStorageKey() { return storageKey; }
    public String getRecordType() { return recordType; }
    public String getRecordId() { return recordId; }
    public String getPayload() { return payload; }
}
