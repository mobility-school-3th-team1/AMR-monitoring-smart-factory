package com.sfaas.amr_control_system.dto;

import lombok.Data;

@Data
public class AlarmCreateRequestDto {
    private String sourceType;
    private String sourceId;
    private String level;
    private String message;
}
