package com.sfaas.amr_control_system.dto;

import lombok.Data;

@Data
public class ChargingStationAmrDto {
    private String amrId;
    private String amrName;
    private Integer batteryPercent;
    private String eta;
}
