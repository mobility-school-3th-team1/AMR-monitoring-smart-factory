package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AmrStatusDto {
    private String amrId;
    private String status;
    private PositionDto position;
    private Integer batteryPercent;
    private LocalDateTime timestamp;
}