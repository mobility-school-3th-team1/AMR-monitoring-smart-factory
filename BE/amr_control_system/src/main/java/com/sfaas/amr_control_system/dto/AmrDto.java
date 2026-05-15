package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AmrDto {
    private String id;
    private String name;
    private String status;
    private Integer batteryPercent;
    private PositionDto position;
    private PositionDto destination;
    private String currentTask;
    private LocalDateTime lastSeenAt;
}