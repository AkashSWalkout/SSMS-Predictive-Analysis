package com.walkouttech.ssms.service.predictive;

import com.walkouttech.ssms.request.predictive.BulkPredictionRequestDTO;
import com.walkouttech.ssms.request.predictive.SinglePredictionRequestDTO;
import com.walkouttech.ssms.request.predictive.SmsAlertConfigDTO;
import com.walkouttech.ssms.response.predictive.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PredictiveAnalysisService {

    // Single student analysis
    PredictionReportResponseDTO analyseStudent(SinglePredictionRequestDTO request);

    // Bulk analysis (by class or student list)
    List<PredictionReportResponseDTO> analyseBulk(BulkPredictionRequestDTO request);

    // CSV/Excel upload and analyse
    BatchUploadResponseDTO uploadAndAnalyse(MultipartFile file);

    // Get prediction history for a student
    List<PredictionReportResponseDTO> getStudentPredictions(Long studentId);

    // Get single prediction report by ID
    PredictionReportResponseDTO getPredictionById(Long id);

    // Dashboard aggregation
    PredictionDashboardResponseDTO getDashboard();

    // Chart data endpoints
    ChartDataResponseDTO getRiskDistributionChart();
    ChartDataResponseDTO getCategoryBreakdownChart();
    ChartDataResponseDTO getClassWisePerformanceChart();
    ChartDataResponseDTO getStudentTrendChart(Long studentId);

    // SMS alerts
    List<SmsAlertResponseDTO> getTriggeredAlerts();
    List<SmsAlertResponseDTO> configureAndTriggerAlerts(SmsAlertConfigDTO config);

    // CSV template download
    byte[] getCsvTemplate();

    // AI Vision: analyze report card image
    ReportCardVisionResponseDTO analyzeReportCardImage(MultipartFile file);

    // AI analysis: image, text, CSV, or JSON student data file
    ReportCardVisionResponseDTO analyzeStudentDataFile(MultipartFile file, String docType);
}
