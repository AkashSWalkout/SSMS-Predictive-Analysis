package com.walkouttech.ssms.response.predictive;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataResponseDTO {

    private String chartType; // BAR, PIE, LINE, RADAR, DOUGHNUT

    private String title;

    private List<String> labels;

    private List<DatasetDTO> datasets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetDTO {
        private String label;
        private List<Double> data;
        private List<String> backgroundColor;
        private String borderColor;
    }
}
