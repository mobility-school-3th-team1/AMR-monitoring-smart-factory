package com.sfaas.amr_control_system.controller;

import com.sfaas.amr_control_system.dto.AnalyticsBatteryDto;
import com.sfaas.amr_control_system.dto.AnalyticsKpiDto;
import com.sfaas.amr_control_system.dto.AnalyticsWorkloadDto;
import com.sfaas.amr_control_system.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/kpis")
    public ResponseEntity<List<AnalyticsKpiDto>> getKpis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String groupBy
    ) {
        return ResponseEntity.ok(analyticsService.getKpis(from, to, groupBy));
    }

    @GetMapping("/battery")
    public ResponseEntity<List<AnalyticsBatteryDto>> getBattery(
            @RequestParam(required = false) String amrId,
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String groupBy
    ) {
        return ResponseEntity.ok(analyticsService.getBattery(amrId, stationId, from, to, groupBy));
    }

    @GetMapping("/workload")
    public ResponseEntity<List<AnalyticsWorkloadDto>> getWorkload(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String groupBy
    ) {
        return ResponseEntity.ok(analyticsService.getWorkload(from, to, groupBy));
    }
}
