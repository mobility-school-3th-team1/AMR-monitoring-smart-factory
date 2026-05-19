package com.sfaas.amr_control_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmrStatusUpdatedEventDataDto {

    private String amrId;
    private String status;
    private String faultCode;
}
