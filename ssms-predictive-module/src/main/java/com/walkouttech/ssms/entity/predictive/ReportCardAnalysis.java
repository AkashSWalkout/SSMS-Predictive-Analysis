package com.walkouttech.ssms.entity.predictive;

import com.walkouttech.ssms.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_card_analyses")
@Data
public class ReportCardAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "class_name")
    private String className;

    @Column(name = "roll_number")
    private String rollNumber;

    @Column(name = "overall_percentage")
    private Double overallPercentage;

    @Column(name = "overall_grade")
    private String overallGrade;

    /** Subject data stored as JSON array string */
    @Column(name = "subjects_json", columnDefinition = "TEXT")
    private String subjectsJson;

    /** Attendance data stored as JSON object string */
    @Column(name = "attendance_json", columnDefinition = "TEXT")
    private String attendanceJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "performance_summary", columnDefinition = "TEXT")
    private String performanceSummary;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    /** Exam-specific analysis stored as JSON (handwriting, conceptual gaps, etc.) */
    @Column(name = "exam_analysis_json", columnDefinition = "TEXT")
    private String examAnalysisJson;

    /** Original file name that was uploaded */
    @Column(name = "source_file_name")
    private String sourceFileName;

    /** report_card | exam_paper | text_data */
    @Column(name = "doc_type")
    private String docType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
