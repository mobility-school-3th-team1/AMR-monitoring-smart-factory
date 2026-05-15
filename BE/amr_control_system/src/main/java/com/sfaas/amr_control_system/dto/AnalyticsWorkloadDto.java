package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnalyticsWorkloadDto {
    private LocalDateTime timestamp;
    private String amrId;
    private String taskType;
    private Integer taskCount;
}