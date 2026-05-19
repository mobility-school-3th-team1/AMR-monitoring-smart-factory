package com.sfaas.amr_control_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamEventDto {

    private String event;
    private String timestamp;
    private Object data;
}
