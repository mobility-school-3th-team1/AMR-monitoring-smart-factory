package com.sfaas.amr_control_system.controller;

import com.sfaas.amr_control_system.dto.AmrCommandRequestDto;
import com.sfaas.amr_control_system.dto.AmrCommandResponseDto;
import com.sfaas.amr_control_system.dto.AmrDto;
import com.sfaas.amr_control_system.dto.AmrListResponseDto;
import com.sfaas.amr_control_system.dto.AmrPathDto;
import com.sfaas.amr_control_system.dto.AmrStatusHistoryDto;
import com.sfaas.amr_control_system.service.AmrService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/amrs")
@RequiredArgsConstructor
public class AmrController {

    private final AmrService amrService;

    @GetMapping
    public ResponseEntity<AmrListResponseDto> listAmrs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer batteryMin,
            @RequestParam(required = false) Integer batteryMax,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(amrService.listAmrs(page, limit, status, batteryMin, batteryMax, search, sort));
    }

    @GetMapping("/{amrId}")
    public ResponseEntity<AmrDto> getAmr(@PathVariable String amrId) {
        return ResponseEntity.ok(amrService.getAmr(amrId));
    }

    @GetMapping("/{amrId}/status-history")
    public ResponseEntity<AmrStatusHistoryDto> getStatusHistory(
            @PathVariable String amrId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String groupBy
    ) {
        return ResponseEntity.ok(amrService.getStatusHistory(amrId, from, to, groupBy));
    }

    @GetMapping("/{amrId}/path")
    public ResponseEntity<AmrPathDto> getPath(
            @PathVariable String amrId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(amrService.getPath(amrId, from, to));
    }

    @PostMapping("/{amrId}/commands")
    public ResponseEntity<AmrCommandResponseDto> sendCommand(
            @PathVariable String amrId,
            @RequestBody AmrCommandRequestDto request
    ) {
        return ResponseEntity.ok(amrService.sendCommand(amrId, request));
    }
}
