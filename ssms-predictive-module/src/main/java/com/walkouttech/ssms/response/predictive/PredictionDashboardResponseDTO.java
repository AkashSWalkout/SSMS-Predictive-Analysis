package com.walkouttech.ssms.response.predictive;

import com.walkouttech.ssms.enums.RiskLevel;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PredictionDashboardResponseDTO {

    private int totalStudentsAnalysed;

    // For pie chart: risk level → count
    private Map<RiskLevel, Long> riskDistribution;

    // For bar chart: category name → count
    private Map<String, Long> categoryBreakdown;

    // Top 10 at-risk students
    private List<PredictionReportResponseDTO> topAtRiskStudents;

    // Overall stats
    private Double averagePredictedScore;
    private Double averageConfidence;

    // For grouped bar chart: class name → risk breakdown
    private List<ClassRiskDTO> classWiseRiskData;

    @Data
    public static class ClassRiskDTO {
        private String className;
        private Long lowCount;
        private Long moderateCount;
        private Long highCount;
        private Long criticalCount;
    }
}
