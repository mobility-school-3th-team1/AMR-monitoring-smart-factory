package com.sfaas.amr_control_system.controller;

import com.sfaas.amr_control_system.dto.ChargingForecastDto;
import com.sfaas.amr_control_system.dto.ChargingHistoryResponseDto;
import com.sfaas.amr_control_system.dto.ChargingQueueResponseDto;
import com.sfaas.amr_control_system.dto.ChargingStationDto;
import com.sfaas.amr_control_system.dto.ChargingStationListDto;
import com.sfaas.amr_control_system.service.ChargingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/charging")
@RequiredArgsConstructor
public class ChargingController {

    private final ChargingService chargingService;

    @GetMapping("/stations")
    public ResponseEntity<ChargingStationListDto> listStations() {
        return ResponseEntity.ok(chargingService.listStations());
    }

    @GetMapping("/stations/{stationId}")
    public ResponseEntity<ChargingStationDto> getStation(@PathVariable String stationId) {
        return ResponseEntity.ok(chargingService.getStation(stationId));
    }

    @GetMapping("/queue")
    public ResponseEntity<ChargingQueueResponseDto> getQueue(
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(chargingService.getQueue(stationId, status));
    }

    @GetMapping("/forecast")
    public ResponseEntity<ChargingForecastDto> getForecast(
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String groupBy
    ) {
        return ResponseEntity.ok(chargingService.getForecast(stationId, from, to, groupBy));
    }

    @GetMapping("/history")
    public ResponseEntity<ChargingHistoryResponseDto> getHistory(
            @RequestParam(required = false) String amrId,
            @RequestParam(required = false) String stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String groupBy
    ) {
        return ResponseEntity.ok(chargingService.getHistory(amrId, stationId, from, to, groupBy));
    }
}
