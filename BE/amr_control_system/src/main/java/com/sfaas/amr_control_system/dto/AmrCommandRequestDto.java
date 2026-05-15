package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AmrCommandRequestDto {
    private String command;
    private Map<String, Object> params;
}