package com.walkouttech.ssms.entity.predictive;

import com.walkouttech.ssms.enums.PredictionCategory;
import com.walkouttech.ssms.enums.PredictionStatus;
import com.walkouttech.ssms.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_reports")
@Data
public class PredictionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "student_code")
    private String studentCode;

    @Column(name = "class_name")
    private String className;

    @Column(name = "roll_no")
    private String rollNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private PredictionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PredictionStatus status;

    @Column(name = "predicted_score")
    private Double predictedScore;

    @Column(name = "predicted_grade")
    private String predictedGrade;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
