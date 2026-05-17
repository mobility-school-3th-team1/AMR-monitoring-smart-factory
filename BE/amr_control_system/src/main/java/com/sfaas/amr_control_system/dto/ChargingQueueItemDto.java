package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChargingQueueItemDto {
    private String id;
    private String amrId;
    private String amrName;
    private String stationId;
    private String stationName;
    private String status;
    private Integer batteryPercent;
    private LocalDateTime queuedAt;
}
