package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChargingHistoryItemDto {
    private String id;
    private String amrId;
    private String stationId;
    private String stationName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer startBatteryPercent;
    private Integer endBatteryPercent;
    private String sessionStatus;
}
