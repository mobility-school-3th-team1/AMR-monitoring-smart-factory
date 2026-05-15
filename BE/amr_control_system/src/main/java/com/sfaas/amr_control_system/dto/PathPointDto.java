package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PathPointDto {
    private LocalDateTime time;
    private Double x;
    private Double y;
    private String zone;
}