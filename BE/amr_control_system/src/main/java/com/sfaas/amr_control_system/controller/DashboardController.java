package com.sfaas.amr_control_system.controller;

import com.sfaas.amr_control_system.dto.DashboardRecentLogsResponseDto;
import com.sfaas.amr_control_system.dto.DashboardSummaryDto;
import com.sfaas.amr_control_system.dto.RecentAlarmsDto;
import com.sfaas.amr_control_system.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/recent-alarms")
    public ResponseEntity<RecentAlarmsDto> getRecentAlarms(
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(dashboardService.getRecentAlarms(limit));
    }

    @GetMapping("/recent-logs")
    public ResponseEntity<DashboardRecentLogsResponseDto> getRecentLogs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(dashboardService.getRecentLogs(page, limit));
    }
}
