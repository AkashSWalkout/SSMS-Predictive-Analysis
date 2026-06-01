# SSMS Predictive Analysis Module — Integration Guide

## Overview

This is a **standalone module** containing all source files for the "Predictive Analysis" feature.
It uses **the same code style, patterns, and conventions** as the main SSMS project:
- Lombok (`@Data`, `@RequiredArgsConstructor`)
- JPA entities with `@Entity`/`@Table`
- Service → ServiceImpl pattern
- Request/Response DTOs
- `ApiException` for error handling
- S3 uploads via `S3ServiceUtil`

**No files in the main project are modified.** You copy these files in and make 3 small additions.

---

## Folder Structure

```
ssms-predictive-module/
└── src/main/java/com/walkouttech/ssms/
    ├── controller/predictive/
    │   └── PredictiveAnalysisController.java
    ├── entity/predictive/
    │   ├── PredictionReport.java
    │   ├── PredictionBatchUpload.java
    │   └── SmsAlertTrigger.java
    ├── enums/
    │   ├── PredictionCategory.java
    │   ├── PredictionStatus.java
    │   └── RiskLevel.java
    ├── repository/predictive/
    │   ├── PredictionReportRepository.java
    │   ├── PredictionBatchUploadRepository.java
    │   └── SmsAlertTriggerRepository.java
    ├── request/predictive/
    │   ├── SinglePredictionRequestDTO.java
    │   ├── BulkPredictionRequestDTO.java
    │   └── SmsAlertConfigDTO.java
    ├── response/predictive/
    │   ├── PredictionReportResponseDTO.java
    │   ├── PredictionDashboardResponseDTO.java
    │   ├── ChartDataResponseDTO.java
    │   ├── BatchUploadResponseDTO.java
    │   └── SmsAlertResponseDTO.java
    ├── service/predictive/
    │   ├── PredictiveAnalysisService.java
    │   └── AiClientService.java
    ├── serviceImpl/predictive/
    │   ├── PredictiveAnalysisServiceImpl.java
    │   └── AiClientServiceImpl.java
    └── util/
        └── CsvParserUtil.java
```

---

## Integration Steps (3 steps)

### Step 1: Copy Files

Copy the contents of `ssms-predictive-module/src/` into the main project's `src/` directory:

```powershell
# From the NF_SSMS-API root directory:
Copy-Item -Path ".\ssms-predictive-module\src\*" -Destination ".\NF_SSMS-API\src\" -Recurse -Force
```

This merges the `predictive` sub-packages into the existing package structure. No existing files are overwritten because all new files are in `/predictive/` sub-packages.

### Step 2: Add Dependencies to `pom.xml`

Add the following inside the `<dependencies>` section of your `pom.xml`:

```xml
<!-- OpenCSV for CSV parsing (Predictive Analysis) -->
<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.9</version>
</dependency>

<!-- Apache POI for Excel parsing (Predictive Analysis) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

### Step 3: Add Config to `application.properties`

Append the following to your `application.properties`:

```properties
# ===============================
# Predictive Analysis AI Config
# ===============================
predictive.ai.api-url=${PREDICTIVE_AI_API_URL:https://api.openai.com/v1/chat/completions}
predictive.ai.api-key=${PREDICTIVE_AI_API_KEY:your-api-key-here}
predictive.ai.model=${PREDICTIVE_AI_MODEL:gpt-4}
predictive.ai.max-tokens=${PREDICTIVE_AI_MAX_TOKENS:4096}
predictive.ai.temperature=${PREDICTIVE_AI_TEMPERATURE:0.3}

# File upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### Step 3b: Add one method to existing HomeworkSubmissionRepository

The predictive module needs a `findByStudentId` method that doesn't exist yet in the main project's
`HomeworkSubmissionRepository`. Add this one line to:
`src/main/java/com/walkouttech/ssms/repository/homework/HomeworkSubmissionRepository.java`

```java
List<HomeworkSubmission> findByStudentId(Long studentId);
```

---

## What Happens After Integration

1. **Run the app:** `.\mvnw spring-boot:run`
2. **JPA auto-creates 3 new tables** in your MySQL database:
   - `prediction_reports`
   - `prediction_batch_uploads`
   - `sms_alert_triggers`
3. **Swagger UI** at `http://localhost:9090/swagger-ui/index.html` will show all new `/api/predictive/**` endpoints.

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/predictive/analyse/student` | Analyse single student |
| POST | `/api/predictive/analyse/bulk` | Analyse multiple students |
| POST | `/api/predictive/upload` | Upload CSV/Excel & analyse |
| GET | `/api/predictive/upload/template` | Download CSV template |
| GET | `/api/predictive/student/{id}` | Student's prediction history |
| GET | `/api/predictive/report/{id}` | Single prediction report |
| GET | `/api/predictive/dashboard` | Aggregated dashboard data |
| GET | `/api/predictive/charts/risk-distribution` | Pie/doughnut chart data |
| GET | `/api/predictive/charts/category-breakdown` | Bar chart data |
| GET | `/api/predictive/charts/class-performance` | Class comparison chart |
| GET | `/api/predictive/charts/student-trend/{id}` | Student trend line chart |
| GET | `/api/predictive/alerts` | List all SMS alerts |
| POST | `/api/predictive/alerts/configure` | Set thresholds & trigger alerts |

---

## AI API Configuration

The module uses an **OpenAI-compatible** chat completions API by default. To switch providers:

| Provider | `PREDICTIVE_AI_API_URL` | `PREDICTIVE_AI_MODEL` |
|----------|------------------------|----------------------|
| OpenAI | `https://api.openai.com/v1/chat/completions` | `gpt-4` |
| Gemini | `https://generativelanguage.googleapis.com/v1beta/...` | `gemini-pro` |
| Claude | `https://api.anthropic.com/v1/messages` | `claude-3-sonnet` |

For non-OpenAI providers, modify `AiClientServiceImpl.java` to match their request/response format.

**Dev/Demo Mode:** If no API key is set, the system returns realistic mock predictions so you can develop and test the frontend without burning API credits.

---

## CSV Upload Template

Download via `GET /api/predictive/upload/template`

```csv
student_id,login_frequency,forum_posts,avg_study_hours,extra_curricular,notes
1,5,12,3.5,YES,Good participation
```

The `student_id` column is required and must match existing student IDs in the database.

---

## Prediction Categories

| Category | What it predicts |
|----------|-----------------|
| **ACADEMIC** | Exam scores, pass/fail, grade category, GPA forecast |
| **ENGAGEMENT** | Deadline miss risk, attendance forecast, participation drop |
| **RISK** | Dropout risk, transfer risk, probation risk, course withdrawal |
| **WELLBEING** | Stress/burnout risk, tutoring need, financial aid jeopardy |
| **CAREER** | Graduation on time, employment probability, further study |

---

## SMS Alert Thresholds

Configure via `POST /api/predictive/alerts/configure`:

```json
{
    "dropoutRiskThreshold": 70.0,
    "failRiskScoreThreshold": 50.0,
    "attendanceThreshold": 60.0,
    "deadlineMissThreshold": 30.0,
    "notifyParent": true,
    "notifyStudent": true,
    "notifyTeacher": true
}
```

SMS alerts are **stored in the database** but NOT actually sent (no SMS gateway integrated).
Wire up Twilio/AWS SNS in `SmsAlertTrigger` processing to send real SMS messages.
