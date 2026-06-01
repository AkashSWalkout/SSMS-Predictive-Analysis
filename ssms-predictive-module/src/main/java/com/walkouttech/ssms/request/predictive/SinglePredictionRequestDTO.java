package com.walkouttech.ssms.request.predictive;

import com.walkouttech.ssms.enums.PredictionCategory;
import lombok.Data;

import java.util.List;

@Data
public class SinglePredictionRequestDTO {

    private Long studentId;

    private List<PredictionCategory> categories;
}
