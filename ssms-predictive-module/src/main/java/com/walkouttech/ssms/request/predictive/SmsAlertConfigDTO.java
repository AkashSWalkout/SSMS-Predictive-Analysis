package com.walkouttech.ssms.request.predictive;

import lombok.Data;

@Data
public class SmsAlertConfigDTO {

    private Double dropoutRiskThreshold = 70.0;

    private Double failRiskScoreThreshold = 50.0;

    private Double attendanceThreshold = 60.0;

    private Double deadlineMissThreshold = 30.0;

    private Boolean notifyParent = true;

    private Boolean notifyStudent = true;

    private Boolean notifyTeacher = true;
}
