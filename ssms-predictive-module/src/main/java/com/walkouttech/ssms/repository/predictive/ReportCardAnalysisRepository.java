package com.walkouttech.ssms.repository.predictive;

import com.walkouttech.ssms.entity.predictive.ReportCardAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportCardAnalysisRepository extends JpaRepository<ReportCardAnalysis, Long> {

    List<ReportCardAnalysis> findByStudentNameOrderByCreatedAtDesc(String studentName);

    List<ReportCardAnalysis> findByDocTypeOrderByCreatedAtDesc(String docType);

    List<ReportCardAnalysis> findAllByOrderByCreatedAtDesc();
}
