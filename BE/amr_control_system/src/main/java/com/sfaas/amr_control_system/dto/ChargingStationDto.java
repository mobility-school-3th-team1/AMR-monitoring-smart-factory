package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChargingStationDto {
    private String id;
    private String name;
    private String location;
    private String status;
    private Integer capacity;
    private Integer occupiedCount;
    private Integer averageBatteryPercent;
    private LocalDateTime estimatedFullChargeAt;
}