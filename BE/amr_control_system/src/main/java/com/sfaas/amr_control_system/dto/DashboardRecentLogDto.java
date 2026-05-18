package com.sfaas.amr_control_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DashboardRecentLogDto {
    private String id;
    private String sourceType;
    private String sourceId;
    private String level;
    private String message;
    private LocalDateTime occurredAt;
}
