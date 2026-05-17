package com.sfaas.amr_control_system.controller;

import com.sfaas.amr_control_system.dto.WorkHistoryDto;
import com.sfaas.amr_control_system.dto.WorkHistoryListResponseDto;
import com.sfaas.amr_control_system.service.WorkHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/work-histories")
@RequiredArgsConstructor
public class WorkHistoryController {

    private final WorkHistoryService workHistoryService;

    @GetMapping
    public ResponseEntity<WorkHistoryListResponseDto> listWorkHistories(
            @RequestParam(required = false) String amrId,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(workHistoryService.listWorkHistories(
                amrId, taskType, from, to, result, page, limit
        ));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportWorkHistories(
            @RequestParam(required = false) String amrId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String taskType
    ) {
        byte[] csvContent = workHistoryService.exportWorkHistories(amrId, from, to, taskType);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + workHistoryService.buildBulkExportFileName() + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvContent);
    }

    @GetMapping("/{workHistoryId}")
    public ResponseEntity<WorkHistoryDto> getWorkHistory(@PathVariable String workHistoryId) {
        return ResponseEntity.ok(workHistoryService.getWorkHistory(workHistoryId));
    }

    @GetMapping("/{workHistoryId}/export")
    public ResponseEntity<byte[]> exportWorkHistory(@PathVariable String workHistoryId) {
        byte[] csvContent = workHistoryService.exportWorkHistory(workHistoryId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + workHistoryService.buildExportFileName(workHistoryId) + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvContent);
    }
}
