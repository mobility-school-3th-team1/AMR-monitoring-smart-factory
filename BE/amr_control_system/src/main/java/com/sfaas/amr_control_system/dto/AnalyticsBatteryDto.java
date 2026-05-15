package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnalyticsBatteryDto {
    private LocalDateTime timestamp;
    private String amrId;
    private Integer batteryPercent;
}