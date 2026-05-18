package com.sfaas.amr_control_system.controller;

import com.sfaas.amr_control_system.dto.AlarmAckRequestDto;
import com.sfaas.amr_control_system.dto.AlarmAckResponseDto;
import com.sfaas.amr_control_system.dto.AlarmCreateRequestDto;
import com.sfaas.amr_control_system.dto.AlarmDto;
import com.sfaas.amr_control_system.dto.AlarmListResponseDto;
import com.sfaas.amr_control_system.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping
    public ResponseEntity<AlarmListResponseDto> listAlarms(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Boolean acknowledged,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(alarmService.listAlarms(
                level, acknowledged, sourceType, sourceId, from, to, page, limit
        ));
    }

    @GetMapping("/{alarmId}")
    public ResponseEntity<AlarmDto> getAlarm(@PathVariable String alarmId) {
        return ResponseEntity.ok(alarmService.getAlarm(alarmId));
    }

    @PostMapping("/{alarmId}/ack")
    public ResponseEntity<AlarmAckResponseDto> acknowledgeAlarm(
            @PathVariable String alarmId,
            @RequestBody(required = false) AlarmAckRequestDto request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "unknown";
        return ResponseEntity.ok(alarmService.acknowledgeAlarm(alarmId, request, username));
    }

    @PostMapping
    public ResponseEntity<AlarmDto> createAlarm(@RequestBody AlarmCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alarmService.createAlarm(request));
    }
}
