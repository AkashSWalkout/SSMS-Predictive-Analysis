package com.walkouttech.ssms.response.predictive;

import com.walkouttech.ssms.enums.PredictionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BatchUploadResponseDTO {

    private Long id;
    private String fileName;
    private Integer totalRecords;
    private Integer processedRecords;
    private Integer failedRecords;
    private PredictionStatus status;
    private String errorLog;
    private LocalDateTime createdAt;
}
