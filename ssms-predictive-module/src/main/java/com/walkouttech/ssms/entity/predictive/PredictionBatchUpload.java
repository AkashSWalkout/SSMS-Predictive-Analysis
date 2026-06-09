package com.walkouttech.ssms.entity.predictive;

import com.walkouttech.ssms.enums.PredictionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_batch_uploads")
@Data
public class PredictionBatchUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "processed_records")
    private Integer processedRecords;

    @Column(name = "failed_records")
    private Integer failedRecords;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PredictionStatus status;

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
