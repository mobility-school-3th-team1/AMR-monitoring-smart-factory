package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkHistoryDto {
    private String id;
    private String amrId;
    private String taskType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String from;
    private String to;
    private String result;
}