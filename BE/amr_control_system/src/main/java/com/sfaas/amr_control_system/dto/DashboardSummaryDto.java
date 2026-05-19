package com.sfaas.amr_control_system.dto;

import lombok.Data;

@Data
public class DashboardSummaryDto {
    private Integer productionCount;
    private Integer activeAlarms;
    private Integer amrOperating;
    private Integer amrCharging;
    private Integer amrWaiting;
    private Integer amrError;
    private Integer amrErrorUnresolved;
    private Integer avgBatteryPercent;
    private Double averageTaskTimeMin;
}