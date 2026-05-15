package com.sfaas.amr_control_system.dto;

import lombok.Data;

@Data
public class AmrCommandResponseDto {
    private Boolean accepted;
    private String commandId;
    private String amrId;
}