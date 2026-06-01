package com.walkouttech.ssms.response.predictive;

import com.walkouttech.ssms.enums.PredictionCategory;
import com.walkouttech.ssms.enums.PredictionStatus;
import com.walkouttech.ssms.enums.RiskLevel;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PredictionReportResponseDTO {

    // Student Info
    private Long studentId;
    private String studentName;
    private String studentCode;
    private String className;
    private String rollNo;

    // Prediction Info
    private Long reportId;
    private PredictionCategory category;
    private RiskLevel riskLevel;
    private PredictionStatus status;

    // Scores
    private Double predictedScore;
    private String predictedGrade;
    private Double confidence;

    // AI-generated insights
    private String summary;
    private String recommendations;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
