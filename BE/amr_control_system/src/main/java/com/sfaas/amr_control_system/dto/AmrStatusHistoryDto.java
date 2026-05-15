package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.util.List;

@Data
public class AmrStatusHistoryDto {
    private List<AmrStatusDto> data;
}