package com.walkouttech.ssms.repository.predictive;

import com.walkouttech.ssms.entity.predictive.PredictionBatchUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PredictionBatchUploadRepository extends JpaRepository<PredictionBatchUpload, Long> {
}
