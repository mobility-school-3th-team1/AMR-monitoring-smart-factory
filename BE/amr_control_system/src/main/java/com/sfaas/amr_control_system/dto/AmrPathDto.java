package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.util.List;

@Data
public class AmrPathDto {
    private String amrId;
    private List<PathPointDto> path;
}