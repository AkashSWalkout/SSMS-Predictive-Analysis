package com.walkouttech.ssms.controller.predictive;

import com.walkouttech.ssms.request.predictive.BulkPredictionRequestDTO;
import com.walkouttech.ssms.request.predictive.SinglePredictionRequestDTO;
import com.walkouttech.ssms.response.predictive.*;
import com.walkouttech.ssms.service.predictive.PredictiveAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/predictive")
@RequiredArgsConstructor
public class PredictiveAnalysisController {

    private final PredictiveAnalysisService service;

    // ================= ANALYSE =================

    @PostMapping("/analyse/student")
    public ResponseEntity<PredictionReportResponseDTO> analyseStudent(
            @RequestBody SinglePredictionRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.analyseStudent(request));
    }

    @PostMapping("/analyse/bulk")
    public ResponseEntity<List<PredictionReportResponseDTO>> analyseBulk(
            @RequestBody BulkPredictionRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.analyseBulk(request));
    }

    // ================= FILE UPLOAD =================

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchUploadResponseDTO> uploadAndAnalyse(
            @RequestPart("file") MultipartFile file) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.uploadAndAnalyse(file));
    }

    // ================= AI IMAGE ANALYSIS =================

    @PostMapping(value = "/analyze-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportCardVisionResponseDTO> analyzeReportCardImage(
            @RequestPart("file") MultipartFile file) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.analyzeReportCardImage(file));
    }

    @PostMapping(value = "/analyze-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportCardVisionResponseDTO> analyzeStudentDataFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "docType", defaultValue = "report_card") String docType) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.analyzeStudentDataFile(file, docType));
    }

    @GetMapping("/scans")
    public ResponseEntity<List<com.walkouttech.ssms.entity.predictive.ReportCardAnalysis>> getAllScans() {
        return ResponseEntity.ok(service.getAllScans());
    }

    @GetMapping("/upload/template")
    public ResponseEntity<byte[]> downloadCsvTemplate() {

        byte[] template = service.getCsvTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
                "predictive_upload_template.csv");

        return new ResponseEntity<>(template, headers, HttpStatus.OK);
    }

    // ================= GET PREDICTIONS =================

    @GetMapping("/student/{id}")
    public ResponseEntity<List<PredictionReportResponseDTO>> getStudentPredictions(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.getStudentPredictions(id));
    }

    @GetMapping("/report/{id}")
    public ResponseEntity<PredictionReportResponseDTO> getPredictionById(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.getPredictionById(id));
    }

    // ================= DASHBOARD =================

    @GetMapping("/dashboard")
    public ResponseEntity<PredictionDashboardResponseDTO> getDashboard() {

        return ResponseEntity.ok(service.getDashboard());
    }

    // ================= CHARTS =================

    @GetMapping("/charts/risk-distribution")
    public ResponseEntity<ChartDataResponseDTO> getRiskDistributionChart() {
        return ResponseEntity.ok(service.getRiskDistributionChart());
    }

    @GetMapping("/charts/category-breakdown")
    public ResponseEntity<ChartDataResponseDTO> getCategoryBreakdownChart() {
        return ResponseEntity.ok(service.getCategoryBreakdownChart());
    }

    @GetMapping("/charts/class-performance")
    public ResponseEntity<ChartDataResponseDTO> getClassWisePerformanceChart() {
        return ResponseEntity.ok(service.getClassWisePerformanceChart());
    }

    @GetMapping("/charts/student-trend/{id}")
    public ResponseEntity<ChartDataResponseDTO> getStudentTrendChart(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getStudentTrendChart(id));
    }
}
