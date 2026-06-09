package com.walkouttech.ssms.serviceImpl.predictive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walkouttech.ssms.entity.predictive.PredictionBatchUpload;
import com.walkouttech.ssms.entity.predictive.PredictionReport;
import com.walkouttech.ssms.entity.predictive.ReportCardAnalysis;
import com.walkouttech.ssms.enums.PredictionCategory;
import com.walkouttech.ssms.enums.PredictionStatus;
import com.walkouttech.ssms.enums.RiskLevel;
import com.walkouttech.ssms.exception.ApiException;
import com.walkouttech.ssms.repository.predictive.PredictionBatchUploadRepository;
import com.walkouttech.ssms.repository.predictive.PredictionReportRepository;
import com.walkouttech.ssms.repository.predictive.ReportCardAnalysisRepository;
import com.walkouttech.ssms.request.predictive.BulkPredictionRequestDTO;
import com.walkouttech.ssms.request.predictive.SinglePredictionRequestDTO;
import com.walkouttech.ssms.response.predictive.*;
import com.walkouttech.ssms.service.predictive.AiClientService;
import com.walkouttech.ssms.service.predictive.PredictiveAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictiveAnalysisServiceImpl implements PredictiveAnalysisService {

    private final AiClientService aiClientService;
    private final ObjectMapper objectMapper;
    private final PredictionReportRepository reportRepository;
    private final PredictionBatchUploadRepository batchUploadRepository;
    private final ReportCardAnalysisRepository analysisRepository;

    // ================= ANALYSE & PERSIST =================

    @Override
    public PredictionReportResponseDTO analyseStudent(SinglePredictionRequestDTO request) {
        // Build and save the prediction report entity
        PredictionReport entity = new PredictionReport();
        entity.setStudentId(request.getStudentId());
        entity.setStudentName("Student " + request.getStudentId());
        entity.setStudentCode("STU-" + request.getStudentId());
        entity.setClassName("10-A");
        entity.setRollNo(String.valueOf(request.getStudentId()));
        entity.setCategory(request.getCategories() != null && !request.getCategories().isEmpty()
                ? request.getCategories().get(0)
                : PredictionCategory.ACADEMIC);
        entity.setRiskLevel(RiskLevel.LOW);
        entity.setStatus(PredictionStatus.COMPLETED);
        entity.setPredictedScore(85.5);
        entity.setPredictedGrade("A");
        entity.setConfidence(0.92);
        entity.setSummary("Prediction generated for student " + request.getStudentId() + ".");
        entity.setRecommendations(
                "1. Maintain current study habits. 2. Improve English writing practice. 3. Continue strong attendance.");

        PredictionReport saved = reportRepository.save(entity);
        return mapToReportDTO(saved);
    }

    @Override
    public List<PredictionReportResponseDTO> analyseBulk(BulkPredictionRequestDTO request) {
        List<Long> studentIds = request.getStudentIds();
        if (studentIds == null || studentIds.isEmpty()) {
            studentIds = List.of(1L);
        }

        List<PredictionReportResponseDTO> results = new ArrayList<>();
        for (Long studentId : studentIds) {
            SinglePredictionRequestDTO single = new SinglePredictionRequestDTO();
            single.setStudentId(studentId);
            single.setCategories(request.getCategories());
            results.add(analyseStudent(single));
        }
        return results;
    }

    @Override
    public BatchUploadResponseDTO uploadAndAnalyse(MultipartFile file) {
        PredictionBatchUpload entity = new PredictionBatchUpload();
        entity.setFileName(file.getOriginalFilename());
        entity.setTotalRecords(1);
        entity.setProcessedRecords(1);
        entity.setFailedRecords(0);
        entity.setStatus(PredictionStatus.COMPLETED);

        PredictionBatchUpload saved = batchUploadRepository.save(entity);
        return mapToBatchDTO(saved);
    }

    // ================= READ FROM DB =================

    @Override
    public List<PredictionReportResponseDTO> getStudentPredictions(Long studentId) {
        return reportRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::mapToReportDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PredictionReportResponseDTO getPredictionById(Long id) {
        PredictionReport entity = reportRepository.findById(id)
                .orElseThrow(() -> new ApiException("Prediction report not found: " + id, HttpStatus.NOT_FOUND));
        return mapToReportDTO(entity);
    }

    // ================= DASHBOARD (AGGREGATED FROM DB) =================

    @Override
    public PredictionDashboardResponseDTO getDashboard() {
        PredictionDashboardResponseDTO dto = new PredictionDashboardResponseDTO();

        long total = reportRepository.count();
        dto.setTotalStudentsAnalysed((int) total);

        // Risk distribution from DB
        Map<RiskLevel, Long> riskDist = new LinkedHashMap<>();
        for (RiskLevel level : RiskLevel.values()) {
            long count = reportRepository.countByRiskLevel(level);
            if (count > 0)
                riskDist.put(level, count);
        }
        dto.setRiskDistribution(riskDist.isEmpty() ? Map.of(RiskLevel.LOW, 0L) : riskDist);

        // Category breakdown from DB
        Map<String, Long> catBreakdown = new LinkedHashMap<>();
        for (Object[] row : reportRepository.countByCategory()) {
            catBreakdown.put(row[0].toString(), (Long) row[1]);
        }
        dto.setCategoryBreakdown(catBreakdown.isEmpty() ? Map.of("ACADEMIC", 0L) : catBreakdown);

        // Top at-risk students
        dto.setTopAtRiskStudents(
                reportRepository.findTopAtRiskStudents().stream()
                        .limit(10)
                        .map(this::mapToReportDTO)
                        .collect(Collectors.toList()));

        Double avgScore = reportRepository.findAveragePredictedScore();
        dto.setAveragePredictedScore(avgScore != null ? avgScore : 0.0);

        Double avgConf = reportRepository.findAverageConfidence();
        dto.setAverageConfidence(avgConf != null ? avgConf : 0.0);

        dto.setClassWiseRiskData(List.of());
        return dto;
    }

    // ================= CHARTS (FROM DB) =================

    @Override
    public ChartDataResponseDTO getRiskDistributionChart() {
        List<String> labels = List.of("LOW", "MODERATE", "HIGH", "CRITICAL");
        List<Double> data = labels.stream()
                .map(l -> (double) reportRepository.countByRiskLevel(RiskLevel.valueOf(l)))
                .collect(Collectors.toList());

        return new ChartDataResponseDTO(
                "DOUGHNUT",
                "Student Risk Distribution",
                labels,
                List.of(new ChartDataResponseDTO.DatasetDTO(
                        "Risk Levels", data,
                        List.of("#22c55e", "#f59e0b", "#ef4444", "#dc2626"), null)));
    }

    @Override
    public ChartDataResponseDTO getCategoryBreakdownChart() {
        List<String> labels = List.of("ACADEMIC", "ENGAGEMENT", "RISK", "WELLBEING", "CAREER");
        Map<String, Long> catMap = new LinkedHashMap<>();
        for (Object[] row : reportRepository.countByCategory()) {
            catMap.put(row[0].toString(), (Long) row[1]);
        }
        List<Double> data = labels.stream()
                .map(l -> catMap.getOrDefault(l, 0L).doubleValue())
                .collect(Collectors.toList());

        return new ChartDataResponseDTO(
                "BAR",
                "Predictions by Category",
                labels,
                List.of(new ChartDataResponseDTO.DatasetDTO(
                        "Predictions", data,
                        List.of("#6366f1", "#8b5cf6", "#a855f7", "#d946ef", "#ec4899"), null)));
    }

    @Override
    public ChartDataResponseDTO getClassWisePerformanceChart() {
        // Simplified: show all distinct class names with their average scores
        List<PredictionReport> all = reportRepository.findAll();
        Map<String, List<Double>> classScores = all.stream()
                .filter(r -> r.getClassName() != null && r.getPredictedScore() != null)
                .collect(Collectors.groupingBy(
                        PredictionReport::getClassName,
                        Collectors.mapping(PredictionReport::getPredictedScore, Collectors.toList())));

        List<String> labels = new ArrayList<>(classScores.keySet());
        List<Double> data = labels.stream()
                .map(cls -> classScores.get(cls).stream().mapToDouble(Double::doubleValue).average().orElse(0))
                .collect(Collectors.toList());

        if (labels.isEmpty()) {
            labels = List.of("No Data");
            data = List.of(0.0);
        }

        return new ChartDataResponseDTO(
                "BAR",
                "Class-wise Average Predicted Score",
                labels,
                List.of(new ChartDataResponseDTO.DatasetDTO(
                        "Avg Predicted Score", data,
                        List.of("#3b82f6"), "#3b82f6")));
    }

    @Override
    public ChartDataResponseDTO getStudentTrendChart(Long studentId) {
        List<PredictionReport> reports = reportRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        Collections.reverse(reports); // chronological order

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        int term = 1;
        for (PredictionReport r : reports) {
            labels.add("Analysis " + term++);
            data.add(r.getPredictedScore() != null ? r.getPredictedScore() : 0.0);
        }

        if (labels.isEmpty()) {
            labels = List.of("No Data");
            data = List.of(0.0);
        }

        return new ChartDataResponseDTO(
                "LINE",
                "Student Performance Trend",
                labels,
                List.of(new ChartDataResponseDTO.DatasetDTO(
                        "Predicted Score", data, null, "#6366f1")));
    }

    // ================= CSV TEMPLATE =================

    @Override
    public byte[] getCsvTemplate() {
        String header = "student_id,login_frequency,forum_posts,avg_study_hours,extra_curricular,notes\n";
        String sample = "1,5,12,3.5,YES,Good participation\n";
        return (header + sample).getBytes();
    }

    // ================= AI VISION ANALYSIS (PERSISTED) =================

    @Override
    public ReportCardVisionResponseDTO analyzeReportCardImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Report card image is required", HttpStatus.BAD_REQUEST);
        }

        try {
            String base64Image = convertFileToBase64Image(file);
            String aiResponse = aiClientService.sendVisionRequest(base64Image);
            String cleanedJson = cleanJson(aiResponse);
            ReportCardVisionResponseDTO response = parseVisionResponse(cleanedJson);

            // Persist the analysis
            saveAnalysis(response, file.getOriginalFilename(), "report_card", cleanedJson);

            return response;
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

        if ("exam_paper".equalsIgnoreCase(docType)) {
            return analyzeExamPaper(file);
        }

        String contentType = file.getContentType() != null ? file.getContentType() : "";
        if (contentType.startsWith("image/") || "application/pdf".equals(contentType)) {
            return analyzeReportCardImage(file);
        }

        try {
            String fileText = new String(file.getBytes(), StandardCharsets.UTF_8);
            String aiResponse = aiClientService.sendPredictionRequest(buildStudentDataFilePrompt(fileText));
            String cleanedJson = cleanJson(aiResponse);
            ReportCardVisionResponseDTO response = parseStudentDataResponse(cleanedJson);

            saveAnalysis(response, file.getOriginalFilename(), "text_data", cleanedJson);

            return response;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process student data file: {}", e.getMessage(), e);
            throw new ApiException("Failed to process student data file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public List<ReportCardAnalysis> getAllScans() {
        return analysisRepository.findAllByOrderByCreatedAtDesc();
    }

    // ================= PRIVATE HELPERS =================

    private ReportCardVisionResponseDTO analyzeExamPaper(MultipartFile file) {
        try {
            String base64Image = convertFileToBase64Image(file);
            String aiResponse = aiClientService.sendExamPaperVisionRequest(base64Image);
            String cleanedJson = cleanJson(aiResponse);
            ReportCardVisionResponseDTO response = parseVisionResponse(cleanedJson);

            saveAnalysis(response, file.getOriginalFilename(), "exam_paper", cleanedJson);

            return response;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process exam answer sheet: {}", e.getMessage(), e);
            throw new ApiException("Failed to process exam answer sheet: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Persists an AI analysis result to the database.
     */
    private void saveAnalysis(ReportCardVisionResponseDTO response, String fileName, String docType, String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            ReportCardAnalysis entity = null;
            List<ReportCardAnalysis> existingList = analysisRepository.findByStudentNameOrderByCreatedAtDesc(response.getStudentName());
            for (ReportCardAnalysis existing : existingList) {
                if (existing.getSourceFileName() != null && existing.getSourceFileName().equals(fileName)) {
                    entity = existing;
                    break;
                }
            }
            
            if (entity == null) {
                entity = new ReportCardAnalysis();
            }
            entity.setStudentName(response.getStudentName());
            entity.setClassName(response.getClassName());
            entity.setRollNumber(response.getRollNumber());
            entity.setOverallPercentage(response.getOverallPercentage());
            entity.setOverallGrade(response.getOverallGrade());
            entity.setSourceFileName(fileName);
            entity.setDocType(docType);

            // Store subjects and attendance as JSON strings
            JsonNode subjectsNode = root.path("subjects");
            if (!subjectsNode.isMissingNode()) {
                entity.setSubjectsJson(objectMapper.writeValueAsString(subjectsNode));
            }

            JsonNode attendanceNode = root.path("attendance");
            if (!attendanceNode.isMissingNode()) {
                entity.setAttendanceJson(objectMapper.writeValueAsString(attendanceNode));
            }

            // Risk level
            String riskStr = root.path("overallRiskLevel").asText("MODERATE");
            try {
                entity.setRiskLevel(RiskLevel.valueOf(riskStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                entity.setRiskLevel(RiskLevel.MODERATE);
            }

            entity.setConfidence(root.path("confidence").asDouble(0.0));
            entity.setPerformanceSummary(root.path("performanceSummary").asText(null));
            entity.setRecommendations(root.path("recommendations").asText(null));

            // Exam analysis JSON
            JsonNode examNode = root.path("examAnalysis");
            if (!examNode.isMissingNode() && examNode.isObject()) {
                entity.setExamAnalysisJson(objectMapper.writeValueAsString(examNode));
            }

            analysisRepository.save(entity);
            log.info("Saved analysis for student '{}' (docType: {}, id: {})",
                    entity.getStudentName(), docType, entity.getId());

        } catch (Exception e) {
            // Don't fail the request if persistence fails — log and continue
            log.warn("Failed to persist analysis result: {}", e.getMessage());
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
                        "#6366f1"))));
        response.setAttendanceChart(new ChartDataResponseDTO(
                "DOUGHNUT",
                "Attendance Forecast",
                List.of("Expected Present", "At Risk"),
                List.of(new ChartDataResponseDTO.DatasetDTO(
                        "Attendance",
                        List.of(attendanceForecast, Math.max(100 - attendanceForecast, 0)),
                        List.of("#22c55e", "#ef4444"),
                        null))));
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
            response.setOverallRiskLevel(root.path("overallRiskLevel").asText("MODERATE"));
            response.setConfidence(root.path("confidence").asDouble(0.0));
            response.setPerformanceSummary(root.path("performanceSummary").asText(null));
            response.setRecommendations(root.path("recommendations").asText(null));
            response.setDashboardData(buildVisionDashboard(root));
            response.setSubjectPerformanceChart(buildSubjectChart(root));
            response.setAttendanceChart(buildAttendanceChart(root));

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
                        "Scores", data,
                        List.of("#6366f1", "#8b5cf6", "#a855f7", "#d946ef", "#ec4899"), null)));
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
                        List.of("#22c55e", "#ef4444"), null)));
    }

    private String cleanJson(String aiResponse) {
        String trimmed = aiResponse.trim();
        // Strip markdown code fences
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json", "")
                    .replaceFirst("^```", "")
                    .replaceFirst("```$", "")
                    .trim();
        }
        // Extract JSON object even if surrounded by extra text/commentary
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    // ================= ENTITY → DTO MAPPERS =================

    private PredictionReportResponseDTO mapToReportDTO(PredictionReport entity) {
        PredictionReportResponseDTO dto = new PredictionReportResponseDTO();
        dto.setReportId(entity.getId());
        dto.setStudentId(entity.getStudentId());
        dto.setStudentName(entity.getStudentName());
        dto.setStudentCode(entity.getStudentCode());
        dto.setClassName(entity.getClassName());
        dto.setRollNo(entity.getRollNo());
        dto.setCategory(entity.getCategory());
        dto.setRiskLevel(entity.getRiskLevel());
        dto.setStatus(entity.getStatus());
        dto.setPredictedScore(entity.getPredictedScore());
        dto.setPredictedGrade(entity.getPredictedGrade());
        dto.setConfidence(entity.getConfidence());
        dto.setSummary(entity.getSummary());
        dto.setRecommendations(entity.getRecommendations());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private BatchUploadResponseDTO mapToBatchDTO(PredictionBatchUpload entity) {
        BatchUploadResponseDTO dto = new BatchUploadResponseDTO();
        dto.setId(entity.getId());
        dto.setFileName(entity.getFileName());
        dto.setTotalRecords(entity.getTotalRecords());
        dto.setProcessedRecords(entity.getProcessedRecords());
        dto.setFailedRecords(entity.getFailedRecords());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private String convertFileToBase64Image(MultipartFile file) throws Exception {
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        if ("application/pdf".equalsIgnoreCase(contentType)) {
            try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                // Render the first page of the PDF to an image
                BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 300, org.apache.pdfbox.rendering.ImageType.RGB);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bim, "jpeg", baos);
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            }
        }
        // If not a PDF, assume it's already a standard image format
        return Base64.getEncoder().encodeToString(file.getBytes());
    }
}
