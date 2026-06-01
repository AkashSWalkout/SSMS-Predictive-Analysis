package com.walkouttech.ssms.response.predictive;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SmsAlertResponseDTO {

    private Long id;
    private String studentName;
    private String studentCode;
    private String recipientType;
    private String recipientPhone;
    private String message;
    private String threshold;
    private Boolean sent;
    private LocalDateTime createdAt;
}
