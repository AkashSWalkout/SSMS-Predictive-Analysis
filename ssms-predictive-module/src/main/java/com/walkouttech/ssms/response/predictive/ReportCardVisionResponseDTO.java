package com.walkouttech.ssms.response.predictive;

import lombok.Data;

import java.util.Map;

@Data
public class ReportCardVisionResponseDTO {

    private String studentName;
    private String className;
    private String rollNumber;
    private Double overallPercentage;
    private String overallGrade;
    private PredictionDashboardResponseDTO dashboardData;
    private ChartDataResponseDTO subjectPerformanceChart;
    private ChartDataResponseDTO attendanceChart;

    // Exam answer sheet analysis data (populated only for exam_paper doc type)
    private Map<String, Object> examAnalysis;
}
