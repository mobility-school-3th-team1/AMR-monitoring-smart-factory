package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnalyticsKpiDto {
    private LocalDateTime timestamp;
    private Integer productionCount;
    private Integer activeAlarms;
    private Integer amrOperating;
    private Integer amrCharging;
    private Integer amrWaiting;
    private Integer avgBatteryPercent;
    private Double averageTaskTimeMin;
}