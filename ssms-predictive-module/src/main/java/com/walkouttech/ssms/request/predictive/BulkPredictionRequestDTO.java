package com.walkouttech.ssms.request.predictive;

import com.walkouttech.ssms.enums.PredictionCategory;
import lombok.Data;

import java.util.List;

@Data
public class BulkPredictionRequestDTO {

    private Long classId;

    private List<Long> studentIds;

    private List<PredictionCategory> categories;
}
