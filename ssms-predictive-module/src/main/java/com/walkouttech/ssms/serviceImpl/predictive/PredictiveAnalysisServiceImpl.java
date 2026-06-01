package com.walkouttech.ssms.serviceImpl.predictive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walkouttech.ssms.enums.PredictionCategory;
import com.walkouttech.ssms.enums.PredictionStatus;
import com.walkouttech.ssms.enums.RiskLevel;
import com.walkouttech.ssms.exception.ApiException;
import com.walkouttech.ssms.request.predictive.BulkPredictionRequestDTO;
import com.walkouttech.ssms.request.predictive.SinglePredictionRequestDTO;
import com.walkouttech.ssms.request.predictive.SmsAlertConfigDTO;
import com.walkouttech.ssms.response.predictive.*;
import com.walkouttech.ssms.service.predictive.AiClientService;
import com.walkouttech.ssms.service.predictive.PredictiveAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictiveAnalysisServiceImpl implements PredictiveAnalysisService {

    private final AiClientService aiClientService;
    private final ObjectMapper objectMapper;

    @Override
    public PredictionReportResponseDTO analyseStudent(SinglePredictionRequestDTO request) {
        PredictionReportResponseDTO dto = new PredictionReportResponseDTO();
        dto.setStudentId(request.getStudentId());
        dto.setStudentName("Demo Student");
        dto.setStudentCode("STU-DEMO");
        dto.setClassName("10-A");
        dto.setRollNo("101");
        dto.setReportId(1L);
        dto.setCategory(PredictionCategory.ACADEMIC);
        dto.setRiskLevel(RiskLevel.LOW);
        dto.setStatus(PredictionStatus.COMPLETED);
        dto.setPredictedScore(85.5);
        dto.setPredictedGrade("A");
        dto.setConfidence(0.92);
        dto.setSummary("Standalone demo prediction generated successfully.");
        dto.setRecommendations("1. Maintain current study habits. 2. Improve English writing practice. 3. Continue strong attendance.");
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    @Override
    public List<PredictionReportResponseDTO> analyseBulk(BulkPredictionRequestDTO request) {
        SinglePredictionRequestDTO single = new SinglePredictionRequestDTO();
        single.setStudentId(1L);
        single.setCategories(request.getCategories());
        return List.of(analyseStudent(single));
    }

    @Override
    public BatchUploadResponseDTO uploadAndAnalyse(MultipartFile file) {
        BatchUploadResponseDTO dto = new BatchUploadResponseDTO();
        dto.setId(1L);
        dto.setFileName(file.getOriginalFilename());
        dto.setTotalRecords(1);
        dto.setProcessedRecords(1);
        dto.setFailedRecords(0);
        dto.setStatus(PredictionStatus.COMPLETED);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    @Override
    public List<PredictionReportResponseDTO> getStudentPredictions(Long studentId) {
        SinglePredictionRequestDTO request = new SinglePredictionRequestDTO();
        request.setStudentId(studentId);
        return List.of(analyseStudent(request));
    }

    @Override
    public PredictionReportResponseDTO getPredictionById(Long id) {
        SinglePredictionRequestDTO request = new SinglePredictionRequestDTO();
        request.setStudentId(1L);
        PredictionReportResponseDTO dto = analyseStudent(request);
        dto.setReportId(id);
        return dto;
    }

    @Override
    public PredictionDashboardResponseDTO getDashboard() {
        PredictionDashboardResponseDTO dto = new PredictionDashboardResponseDTO();
        dto.setTotalStudentsAnalysed(1);
        dto.setRiskDistribution(Map.of(RiskLevel.LOW, 1L));
        dto.setCategoryBreakdown(Map.of("ACADEMIC", 1L));
        dto.setTopAtRiskStudents(List.of());
        dto.setAveragePredictedScore(85.5);
        dto.setAverageConfidence(0.92);
        dto.setClassWiseRiskData(List.of());
        return dto;
    }

    @Override
    public ChartDataResponseDTO getRiskDistributionChart() {
        return new ChartDataResponseDTO(
                "DOUGHNUT",
                "Student Risk Distribution",
                List.of("LOW", "MODERATE", "HIGH", "CRITICAL"),
                List.of(new ChartDataResponseDTO.DatasetDTO(
                        "Risk Levels",
                        List.of(1.0, 0.0, 0.0, 0.0),
                        List.of("#22c55e", "#f59e0b", "#ef4444", "#dc2626"),
                        null
                ))
        );
    }

    @Override
    public ChartDataResponseDTO getCategoryBreakdownChart() {
        return new ChartDataResponseDTO(
                "BAR",
                "Predictions by Category",
                List.of("ACADEMIC", "ENGAGEMENT", "RISK", "WELLBEING", "CAREER"),
                List.of(new ChartDataResponseDTO.DatasetDTO(
                        "Predictions",
                        List.of(1.0, 0.0, 0.0, 0.0, 0.0),
                        List.of("#6366f1", "#8b5cf6", "#a855f7", "#d946ef", "#ec4899"),
                        null
                ))
        );
    }

    @Override
    public ChartDataResponseDTO getClassWisePerformanceChart() {
        return new ChartDataResponseDTO(
                "BAR",
                "Class-wise Average Predicted Score",
                List.of("10-A"),
                List.of(new ChartDataResponseDTO.DatasetDTO(
                        "Avg Predicted Score",
                        List.of(85.5),
                        List.of("#3b82f6"),
                        "#3b82f6"
                ))
        );
    }

    @Override
    public ChartDataResponseDTO getStudentTrendChart(Long studentId) {
        return new ChartDataResponseDTO(
                "LINE",
                "Student Performance Trend",
                List.of("Term 1", "Term 2", "Term 3"),
                List.of(new ChartDataResponseDTO.DatasetDTO(
                        "Predicted Score",
                        List.of(78.0, 82.0, 85.5),
                        null,
                        "#6366f1"
                ))
        );
    }

    @Override
    public List<SmsAlertResponseDTO> getTriggeredAlerts() {
        return List.of();
    }

    @Override
    public List<SmsAlertResponseDTO> configureAndTriggerAlerts(SmsAlertConfigDTO config) {
        return List.of();
    }

    @Override
    public byte[] getCsvTemplate() {
        String header = "student_id,login_frequency,forum_posts,avg_study_hours,extra_curricular,notes\n";
        String sample = "1,5,12,3.5,YES,Good participation\n";
        return (header + sample).getBytes();
    }

    @Override
    public ReportCardVisionResponseDTO analyzeReportCardImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Report card image is required", HttpStatus.BAD_REQUEST);
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String aiResponse = aiClientService.sendVisionRequest(base64Image);
            return parseVisionResponse(cleanJson(aiResponse));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process report card image: {}", e.getMessage(), e);
            throw new ApiException("Failed to process report card image: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ReportCardVisionResponseDTO analyzeStudentDataFile(MultipartFile file, String docType) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Student data file is required", HttpStatus.BAD_REQUEST);
        }

        // Route exam papers to the dedicated exam analysis flow
        if ("exam_paper".equalsIgnoreCase(docType)) {
            return analyzeExamPaper(file);
        }

        String contentType = file.getContentType() != null ? file.getContentType() : "";
        if (contentType.startsWith("image/")) {
            return analyzeReportCardImage(file);
        }

        try {
            String fileText = new String(file.getBytes(), StandardCharsets.UTF_8);
            String aiResponse = aiClientService.sendPredictionRequest(buildStudentDataFilePrompt(fileText));
            return parseStudentDataResponse(cleanJson(aiResponse));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process student data file: {}", e.getMessage(), e);
            throw new ApiException("Failed to process student data file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Analyzes a student's handwritten exam answer sheet using AI Vision.
     * Returns the standard dashboard response enriched with exam-specific
     * analysis (handwriting quality, conceptual strengths/gaps, etc.).
     */
    private ReportCardVisionResponseDTO analyzeExamPaper(MultipartFile file) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String aiResponse = aiClientService.sendExamPaperVisionRequest(base64Image);
            return parseVisionResponse(cleanJson(aiResponse));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process exam answer sheet: {}", e.getMessage(), e);
            throw new ApiException("Failed to process exam answer sheet: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private String buildStudentDataFilePrompt(String fileText) {
        return """
                Analyze the following student data. It may include grades, attendance, homework,
                behavior notes, engagement indicators, teacher observations, or CSV-style records.

                Extract the most useful student performance signals and return ONLY valid JSON.
                Prefer this structure when possible:
                {
                    "studentName": "<student name or Unknown Student>",
                    "className": "<class or N/A>",
                    "rollNumber": "<roll number or N/A>",
                    "overallPercentage": <number 0-100>,
                    "overallGrade": "<grade>",
                    "subjects": [{"name":"Academic", "score":<0-100>, "grade":"<grade>", "totalMarks":100}],
                    "attendance": {"totalDays":<number>, "presentDays":<number>, "percentage":<0-100>},
                    "overallRiskLevel": "<LOW|MODERATE|HIGH|CRITICAL>",
                    "confidence": <0.0-1.0>,
                    "performanceSummary": "<summary>",
                    "recommendations": "<recommendations>"
                }

                Student data:
                %s
                """.formatted(fileText);
    }

    private ReportCardVisionResponseDTO parseStudentDataResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.has("studentName") || root.has("subjects") || root.has("attendance")) {
                return parseVisionResponse(json);
            }
            return mapPredictionJsonToDashboardResponse(root);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse student data AI response: {}", e.getMessage(), e);
            throw new ApiException("Failed to parse AI student data response", HttpStatus.BAD_GATEWAY);
        }
    }

    private ReportCardVisionResponseDTO mapPredictionJsonToDashboardResponse(JsonNode root) {
        JsonNode predictions = root.path("predictions");
        JsonNode academic = predictions.path("academic");
        JsonNode engagement = predictions.path("engagement");
        JsonNode risk = predictions.path("risk");
        JsonNode wellbeing = predictions.path("wellbeing");

        double academicScore = academic.path("predictedScore").asDouble(72.5);
        double attendanceForecast = engagement.path("attendanceForecast").asDouble(78.0);
        double engagementScore = 100 - (engagement.path("participationDropRisk").asDouble(0.15) * 100);
        double riskScore = 100 - (risk.path("dropoutRisk").asDouble(0.12) * 100);
        double wellbeingScore = 100 - (wellbeing.path("stressRisk").asDouble(0.22) * 100);

        ReportCardVisionResponseDTO response = new ReportCardVisionResponseDTO();
        response.setStudentName("Uploaded Student Data");
        response.setClassName("N/A");
        response.setRollNumber("N/A");
        response.setOverallPercentage(academicScore);
        response.setOverallGrade(academic.path("predictedGrade").asText("N/A"));
        response.setDashboardData(buildPredictionDashboard(root, academicScore));
        response.setSubjectPerformanceChart(new ChartDataResponseDTO(
                "RADAR",
                "AI Performance Signals",
                List.of("Academic", "Attendance", "Engagement", "Risk Safety", "Wellbeing"),
                List.of(new ChartDataResponseDTO.DatasetDTO(
                        "Score",
                        List.of(academicScore, attendanceForecast, engagementScore, riskScore, wellbeingScore),
                        List.of("rgba(99, 102, 241, 0.25)"),
                        "#6366f1"
                ))
        ));
        response.setAttendanceChart(new ChartDataResponseDTO(
                "DOUGHNUT",
                "Attendance Forecast",
                List.of("Expected Present", "At Risk"),
                List.of(new ChartDataResponseDTO.DatasetDTO(
                        "Attendance",
                        List.of(attendanceForecast, Math.max(100 - attendanceForecast, 0)),
                        List.of("#22c55e", "#ef4444"),
                        null
                ))
        ));
        return response;
    }

    private PredictionDashboardResponseDTO buildPredictionDashboard(JsonNode root, double academicScore) {
        PredictionDashboardResponseDTO dto = new PredictionDashboardResponseDTO();
        String riskStr = root.path("overallRiskLevel").asText("MODERATE");
        RiskLevel riskLevel = RiskLevel.valueOf(riskStr.toUpperCase());

        dto.setTotalStudentsAnalysed(1);
        dto.setRiskDistribution(Map.of(riskLevel, 1L));
        dto.setCategoryBreakdown(Map.of("ACADEMIC", 1L, "ENGAGEMENT", 1L, "RISK", 1L, "WELLBEING", 1L));
        dto.setTopAtRiskStudents(List.of());
        dto.setAveragePredictedScore(academicScore);
        dto.setAverageConfidence(root.path("confidence").asDouble(0.78));
        dto.setClassWiseRiskData(List.of());
        return dto;
    }

    private ReportCardVisionResponseDTO parseVisionResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            ReportCardVisionResponseDTO response = new ReportCardVisionResponseDTO();
            response.setStudentName(root.path("studentName").asText());
            response.setClassName(root.path("className").asText());
            response.setRollNumber(root.path("rollNumber").asText());
            response.setOverallPercentage(root.path("overallPercentage").asDouble());
            response.setOverallGrade(root.path("overallGrade").asText());
            response.setDashboardData(buildVisionDashboard(root));
            response.setSubjectPerformanceChart(buildSubjectChart(root));
            response.setAttendanceChart(buildAttendanceChart(root));

            // Extract exam analysis data if present (for exam_paper uploads)
            JsonNode examAnalysisNode = root.path("examAnalysis");
            if (!examAnalysisNode.isMissingNode() && examAnalysisNode.isObject()) {
                response.setExamAnalysis(objectMapper.convertValue(examAnalysisNode, Map.class));
            }

            return response;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse vision AI response: {}", e.getMessage(), e);
            throw new ApiException("Failed to parse AI vision response", HttpStatus.BAD_GATEWAY);
        }
    }

    private PredictionDashboardResponseDTO buildVisionDashboard(JsonNode root) {
        PredictionDashboardResponseDTO dto = new PredictionDashboardResponseDTO();
        String riskStr = root.path("overallRiskLevel").asText("MODERATE");
        RiskLevel riskLevel = RiskLevel.valueOf(riskStr.toUpperCase());

        dto.setTotalStudentsAnalysed(1);
        dto.setRiskDistribution(Map.of(riskLevel, 1L));
        dto.setCategoryBreakdown(Map.of("ACADEMIC", 1L));
        dto.setTopAtRiskStudents(List.of());
        dto.setAveragePredictedScore(root.path("overallPercentage").asDouble());
        dto.setAverageConfidence(root.path("confidence").asDouble());
        dto.setClassWiseRiskData(List.of());
        return dto;
    }

    private ChartDataResponseDTO buildSubjectChart(JsonNode root) {
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        JsonNode subjects = root.path("subjects");
        if (subjects.isArray()) {
            for (JsonNode subject : subjects) {
                labels.add(subject.path("name").asText());
                data.add(subject.path("score").asDouble());
            }
        }

        return new ChartDataResponseDTO(
                "BAR",
                "Subject-wise Performance",
                labels,
                List.of(new ChartDataResponseDTO.DatasetDTO(
                        "Scores",
                        data,
                        List.of("#6366f1", "#8b5cf6", "#a855f7", "#d946ef", "#ec4899"),
                        null
                ))
        );
    }

    private ChartDataResponseDTO buildAttendanceChart(JsonNode root) {
        JsonNode attendance = root.path("attendance");
        double present = attendance.path("presentDays").asDouble();
        double total = attendance.path("totalDays").asDouble();
        double absent = Math.max(total - present, 0);

        return new ChartDataResponseDTO(
                "DOUGHNUT",
                "Attendance",
                List.of("Present", "Absent"),
                List.of(new ChartDataResponseDTO.DatasetDTO(
                        "Days",
                        List.of(present, absent),
                        List.of("#22c55e", "#ef4444"),
                        null
                ))
        );
    }

    private String cleanJson(String aiResponse) {
        String trimmed = aiResponse.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json", "")
                    .replaceFirst("^```", "")
                    .replaceFirst("```$", "")
                    .trim();
        }
        return trimmed;
    }
}
