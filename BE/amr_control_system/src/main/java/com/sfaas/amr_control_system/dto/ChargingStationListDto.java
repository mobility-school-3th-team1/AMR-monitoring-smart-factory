package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChargingStationListDto {
    private List<ChargingStationDto> data;
}