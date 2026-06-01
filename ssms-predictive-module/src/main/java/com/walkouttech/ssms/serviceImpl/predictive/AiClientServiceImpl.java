package com.walkouttech.ssms.serviceImpl.predictive;

import com.walkouttech.ssms.exception.ApiException;
import com.walkouttech.ssms.request.predictive.VisionApiRequestDTO;
import com.walkouttech.ssms.service.predictive.AiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Default AI client implementation using OpenAI-compatible chat completions
 * API.
 * Configure via application.properties or environment variables:
 * - predictive.ai.api-url
 * - predictive.ai.api-key
 * - predictive.ai.model
 * - predictive.ai.max-tokens
 * - predictive.ai.temperature
 */
@Service
@Slf4j
public class AiClientServiceImpl implements AiClientService {

    @Value("${predictive.ai.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${predictive.ai.api-key:}")
    private String apiKey;

    @Value("${predictive.ai.model:gpt-4}")
    private String model;

    @Value("${predictive.ai.max-tokens:4096}")
    private int maxTokens;

    @Value("${predictive.ai.temperature:0.3}")
    private double temperature;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String sendPredictionRequest(String prompt) {

        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your-api-key-here")) {
            log.warn("AI API key is not configured. Returning mock response.");
            return getMockResponse();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "temperature", temperature,
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "You are an educational data analyst AI. Analyse student data and return predictions in JSON format."),
                            Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getBody() == null) {
                throw new ApiException("Empty response from AI API", HttpStatus.BAD_GATEWAY);
            }

            // Extract the content from the response
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");

            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }

            return response.getBody().toString();

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI API call failed: {}", e.getMessage(), e);
            throw new ApiException("AI API call failed: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public String sendVisionRequest(String base64Image) {

        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your-api-key-here")) {
            log.warn("AI API key is not configured. Returning mock vision response.");
            return getMockVisionResponse();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            VisionApiRequestDTO request = new VisionApiRequestDTO();
            request.setModel(model);
            request.setMaxTokens(maxTokens);
            request.setTemperature(temperature);

            VisionApiRequestDTO.MessageDTO systemMessage = new VisionApiRequestDTO.MessageDTO();
            systemMessage.setRole("system");
            systemMessage.setContent(
                    "You are an expert AI that extracts student report card data from images, "
                            + "including handwritten, low-quality, blurry, or photographed report cards. "
                            + "You MUST try your best to read ALL text, even if handwriting is messy. "
                            + "IMPORTANT RULES: "
                            + "1) If you see letter grades (A+, A, B+, B, C, D, F etc.), convert them to numeric scores: "
                            + "A+=95, A=90, A-=87, B+=83, B=78, B-=75, C+=73, C=68, C-=65, D=55, F=30. "
                            + "2) NEVER return score: 0 unless the student truly scored zero. If you can see ANY grade, convert it. "
                            + "3) If you cannot read a value, make your best educated guess rather than returning 0. "
                            + "4) If attendance data is not visible, estimate: totalDays=200, presentDays=170, percentage=85. "
                            + "5) If studentName is not visible, use 'Student' as the name. "
                            + "Return a JSON object with: "
                            + "studentName (string), className (string), rollNumber (string), "
                            + "overallPercentage (number 0-100), overallGrade (string), "
                            + "subjects (array of {name, score (NUMBER 0-100), grade, totalMarks:100}), "
                            + "attendance ({totalDays, presentDays, percentage}), "
                            + "overallRiskLevel (LOW|MODERATE|HIGH|CRITICAL), "
                            + "confidence (0.0-1.0), performanceSummary (string), recommendations (string). "
                            + "Return ONLY valid JSON, no markdown fences.");

            VisionApiRequestDTO.MessageDTO userMessage = new VisionApiRequestDTO.MessageDTO();
            userMessage.setRole("user");

            VisionApiRequestDTO.ContentDTO textContent = new VisionApiRequestDTO.ContentDTO();
            textContent.setType("text");
            textContent.setText("Carefully examine this report card image. It may be handwritten or low quality. "
                    + "Read every subject name and its grade/score. Convert letter grades to numeric scores. "
                    + "NEVER return 0 for a score if you can see any grade written. "
                    + "Return ONLY valid JSON.");

            VisionApiRequestDTO.ContentDTO imageContent = new VisionApiRequestDTO.ContentDTO();
            imageContent.setType("image_url");
            VisionApiRequestDTO.ImageUrlDTO imageUrl = new VisionApiRequestDTO.ImageUrlDTO();
            imageUrl.setUrl("data:image/jpeg;base64," + base64Image);
            imageContent.setImageUrl(imageUrl);

            userMessage.setContent(List.of(textContent, imageContent));
            request.setMessages(List.of(systemMessage, userMessage));

            HttpEntity<VisionApiRequestDTO> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getBody() == null) {
                throw new ApiException("Empty response from AI Vision API",
                        HttpStatus.BAD_GATEWAY);
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");

            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }

            return response.getBody().toString();

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI Vision API call failed: {}", e.getMessage(), e);
            throw new ApiException("AI Vision API call failed: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Returns a realistic mock response when no API key is configured.
     * This allows the system to function in demo/development mode.
     */
    private String getMockResponse() {
        return """
                {
                    "predictions": {
                        "academic": {
                            "predictedScore": 72.5,
                            "predictedGrade": "Merit",
                            "passProbability": 0.85,
                            "gpaForecast": 3.2
                        },
                        "engagement": {
                            "deadlineMissRisk": 0.25,
                            "attendanceForecast": 78.0,
                            "participationDropRisk": 0.15,
                            "loginDeclineRisk": 0.10
                        },
                        "risk": {
                            "dropoutRisk": 0.12,
                            "transferRisk": 0.05,
                            "probationRisk": 0.18,
                            "courseWithdrawalRisk": 0.08
                        },
                        "wellbeing": {
                            "stressRisk": 0.22,
                            "tutoringNeed": 0.35,
                            "financialAidRisk": 0.10
                        },
                        "career": {
                            "graduationOnTime": 0.90,
                            "employmentProbability": 0.75,
                            "furtherStudyLikelihood": 0.40
                        }
                    },
                    "overallRiskLevel": "LOW",
                    "confidence": 0.78,
                    "summary": "Student shows consistent academic performance with an upward trend. Attendance is above average. Minor risk of missing upcoming deadlines due to recent pattern of late submissions. Overall outlook is positive.",
                    "recommendations": "1. Encourage consistent study habits to maintain current trajectory. 2. Monitor upcoming assignment deadlines closely. 3. Consider peer study group participation. 4. Schedule mid-term check-in with academic advisor."
                }
                """;
    }

    private String getMockVisionResponse() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return """
                {
                    "studentName": "Alex Johnson",
                    "className": "10-A",
                    "rollNumber": "101",
                    "overallPercentage": 85.5,
                    "overallGrade": "A",
                    "subjects": [
                        {"name": "Mathematics", "score": 92, "grade": "A+", "totalMarks": 100},
                        {"name": "Science", "score": 88, "grade": "A", "totalMarks": 100},
                        {"name": "English", "score": 79, "grade": "B+", "totalMarks": 100},
                        {"name": "History", "score": 85, "grade": "A", "totalMarks": 100},
                        {"name": "Geography", "score": 83, "grade": "B+", "totalMarks": 100}
                    ],
                    "attendance": {
                        "totalDays": 180,
                        "presentDays": 165,
                        "percentage": 91.67
                    },
                    "overallRiskLevel": "LOW",
                    "confidence": 0.92,
                    "performanceSummary": "Alex is performing well above average with consistent scores across all subjects. Strong performance in Mathematics and Science. Attendance is excellent with minimal absences. Overall outlook is very positive.",
                    "recommendations": "1. Continue with current study habits and maintain consistent performance. 2. Consider advanced placement or enrichment programs in Mathematics and Science. 3. Slight improvement needed in English to reach A grade. 4. Encourage participation in academic competitions."
                }
                """;
    }

    @Override
    public String sendExamPaperVisionRequest(String base64Image) {

        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your-api-key-here")) {
            log.warn("AI API key is not configured. Returning mock exam paper response.");
            return getMockExamPaperResponse();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            VisionApiRequestDTO request = new VisionApiRequestDTO();
            request.setModel(model);
            request.setMaxTokens(maxTokens);
            request.setTemperature(temperature);

            VisionApiRequestDTO.MessageDTO systemMessage = new VisionApiRequestDTO.MessageDTO();
            systemMessage.setRole("system");
            systemMessage.setContent(
                    "You are an expert AI teacher/examiner that analyzes student handwritten exam answer sheets. "
                            + "Your job is to READ the student's handwritten answers carefully, even if the handwriting is messy. "
                            + "IMPORTANT: You are NOT grading the paper. You are analyzing the student's conceptual understanding. "
                            + "Analyze each answer for: correctness, conceptual clarity, common mistakes, and problem-solving approach. "
                            + "Return a JSON object with these fields: "
                            + "studentName (string, extract from paper or use 'Student'), "
                            + "className (string or 'N/A'), "
                            + "rollNumber (string or 'N/A'), "
                            + "overallPercentage (number 0-100, your estimated score based on answer quality), "
                            + "overallGrade (string), "
                            + "subjects (array of {name: subject name, score: 0-100, grade: string, totalMarks: 100}), "
                            + "attendance ({totalDays: 200, presentDays: 170, percentage: 85}), "
                            + "overallRiskLevel (LOW|MODERATE|HIGH|CRITICAL based on conceptual gaps), "
                            + "confidence (0.0-1.0, how clearly you could read the handwriting), "
                            + "performanceSummary (string - DETAILED analysis of the student's answers, what they got right, what they got wrong, and WHY), "
                            + "recommendations (string - specific study tips based on the mistakes found), "
                            + "examAnalysis (object with: "
                            + "  handwritingScore (0-100, legibility rating), "
                            + "  handwritingQuality (string: Excellent|Good|Fair|Poor), "
                            + "  handwritingNotes (string, brief comment on presentation), "
                            + "  conceptualStrengths (array of strings, topics the student clearly understands), "
                            + "  conceptualGaps (array of strings, topics where the student has misunderstandings), "
                            + "  detailedAnalysis (string, question-by-question breakdown if possible)"
                            + "). "
                            + "Return ONLY valid JSON, no markdown fences.");

            VisionApiRequestDTO.MessageDTO userMessage = new VisionApiRequestDTO.MessageDTO();
            userMessage.setRole("user");

            VisionApiRequestDTO.ContentDTO textContent = new VisionApiRequestDTO.ContentDTO();
            textContent.setType("text");
            textContent.setText("This is a student's handwritten exam answer sheet. "
                    + "Read every answer carefully. Identify what the student wrote, whether it is correct, "
                    + "and what conceptual misunderstandings they may have. "
                    + "Rate the handwriting legibility. Return ONLY valid JSON.");

            VisionApiRequestDTO.ContentDTO imageContent = new VisionApiRequestDTO.ContentDTO();
            imageContent.setType("image_url");
            VisionApiRequestDTO.ImageUrlDTO imageUrl = new VisionApiRequestDTO.ImageUrlDTO();
            imageUrl.setUrl("data:image/jpeg;base64," + base64Image);
            imageContent.setImageUrl(imageUrl);

            userMessage.setContent(List.of(textContent, imageContent));
            request.setMessages(List.of(systemMessage, userMessage));

            HttpEntity<VisionApiRequestDTO> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getBody() == null) {
                throw new ApiException("Empty response from AI Vision API",
                        HttpStatus.BAD_GATEWAY);
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");

            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }

            return response.getBody().toString();

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI Exam Paper Vision API call failed: {}", e.getMessage(), e);
            throw new ApiException("AI Exam Paper Vision API call failed: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private String getMockExamPaperResponse() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return """
                {
                    "studentName": "Priya Sharma",
                    "className": "9-B",
                    "rollNumber": "214",
                    "overallPercentage": 68.0,
                    "overallGrade": "B",
                    "subjects": [
                        {"name": "Mathematics", "score": 62, "grade": "B", "totalMarks": 100},
                        {"name": "Science", "score": 74, "grade": "B+", "totalMarks": 100}
                    ],
                    "attendance": {
                        "totalDays": 200,
                        "presentDays": 170,
                        "percentage": 85
                    },
                    "overallRiskLevel": "MODERATE",
                    "confidence": 0.72,
                    "performanceSummary": "Priya demonstrates a solid understanding of basic scientific concepts but struggles with mathematical problem-solving, particularly in algebra and word problems. Her science answers show good recall of definitions but lack depth in explanatory questions. In Math Q3, she correctly set up the equation but made a sign error during simplification. In Science Q5, she confused mitosis with meiosis in the diagram labeling.",
                    "recommendations": "1. Practice algebraic sign rules with step-by-step worked examples. 2. Create flashcards to differentiate mitosis vs meiosis phases. 3. Focus on showing full working in math problems to earn partial credit. 4. Use diagram-based revision for biology cell division topics.",
                    "examAnalysis": {
                        "handwritingScore": 65,
                        "handwritingQuality": "Fair",
                        "handwritingNotes": "Handwriting is generally legible but becomes rushed and harder to read towards the end of the paper. Some numbers (6 vs 0) are ambiguous.",
                        "conceptualStrengths": [
                            "Strong understanding of basic arithmetic operations",
                            "Good recall of scientific definitions and terminology",
                            "Correct application of the Pythagorean theorem",
                            "Neat diagram labeling for plant cell structure"
                        ],
                        "conceptualGaps": [
                            "Confusion between mitosis and meiosis phases",
                            "Sign errors in algebraic simplification (negative × negative)",
                            "Difficulty converting word problems into equations",
                            "Incomplete understanding of chemical balancing"
                        ],
                        "detailedAnalysis": "Q1 (Math - Arithmetic): Correctly solved 3/4 parts. Made a calculation error in part (d) with decimal multiplication. Q2 (Math - Algebra): Set up the equation correctly but lost the negative sign when moving terms. Q3 (Math - Geometry): Applied Pythagorean theorem correctly. Q4 (Science - Biology): Correctly labeled plant cell but confused mitosis/meiosis in part (b). Q5 (Science - Chemistry): Balanced 2 out of 3 equations. Struggled with the combustion reaction."
                    }
                }
                """;
    }
}
