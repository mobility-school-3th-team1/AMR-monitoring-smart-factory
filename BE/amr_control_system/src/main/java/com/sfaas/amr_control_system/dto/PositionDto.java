package com.sfaas.amr_control_system.dto;

import lombok.Data;

@Data
public class PositionDto {
    private String zone;
    private Double x;
    private Double y;
}