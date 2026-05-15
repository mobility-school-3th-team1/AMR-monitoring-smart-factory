package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlarmAckResponseDto {
    private Boolean acknowledged;
    private String ackBy;
    private LocalDateTime ackAt;
}