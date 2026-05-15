package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.util.List;

@Data
public class WorkHistoryListResponseDto {
    private List<WorkHistoryDto> data;
    private Integer total;
}