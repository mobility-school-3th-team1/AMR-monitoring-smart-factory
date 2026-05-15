package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.util.List;

@Data
public class AmrListResponseDto {
    private List<AmrDto> data;
    private Integer total;
    private Integer page;
    private Integer limit;
}