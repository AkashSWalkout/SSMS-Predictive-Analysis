package com.walkouttech.ssms.repository.predictive;

import com.walkouttech.ssms.entity.predictive.PredictionReport;
import com.walkouttech.ssms.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionReportRepository extends JpaRepository<PredictionReport, Long> {

    List<PredictionReport> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    long countByRiskLevel(RiskLevel riskLevel);

    @Query("SELECT AVG(p.predictedScore) FROM PredictionReport p")
    Double findAveragePredictedScore();

    @Query("SELECT AVG(p.confidence) FROM PredictionReport p")
    Double findAverageConfidence();

    @Query("SELECT p.category, COUNT(p) FROM PredictionReport p GROUP BY p.category")
    List<Object[]> countByCategory();

    @Query("SELECT p FROM PredictionReport p WHERE p.riskLevel IN ('HIGH', 'CRITICAL') ORDER BY p.predictedScore ASC")
    List<PredictionReport> findTopAtRiskStudents();
}
