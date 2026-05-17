package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.dto.WorkHistoryDto;
import com.sfaas.amr_control_system.dto.WorkHistoryListResponseDto;
import com.sfaas.amr_control_system.entity.AmrTask;
import com.sfaas.amr_control_system.exception.WorkHistoryNotFoundException;
import com.sfaas.amr_control_system.repository.AmrTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkHistoryService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final String CSV_HEADER = "id,amrId,taskType,startTime,endTime,from,to,result";
    private static final DateTimeFormatter CSV_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final AmrTaskRepository amrTaskRepository;

    public WorkHistoryListResponseDto listWorkHistories(
            String amrId,
            String taskType,
            LocalDateTime from,
            LocalDateTime to,
            String result,
            Integer page,
            Integer limit
    ) {
        int resolvedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int resolvedLimit = limit == null || limit < 1 ? DEFAULT_LIMIT : limit;

        List<WorkHistoryDto> filtered = findFilteredWorkHistories(amrId, taskType, from, to, result);

        int fromIndex = Math.min((resolvedPage - 1) * resolvedLimit, filtered.size());
        int toIndex = Math.min(fromIndex + resolvedLimit, filtered.size());
        List<WorkHistoryDto> pageData = filtered.subList(fromIndex, toIndex);

        WorkHistoryListResponseDto response = new WorkHistoryListResponseDto();
        response.setData(pageData);
        response.setTotal(filtered.size());
        return response;
    }

    public WorkHistoryDto getWorkHistory(String workHistoryId) {
        return toWorkHistoryDto(findTaskOrThrow(workHistoryId));
    }

    public byte[] exportWorkHistory(String workHistoryId) {
        WorkHistoryDto workHistory = getWorkHistory(workHistoryId);
        String csvBody = buildCsvRows(List.of(workHistory));
        return csvBody.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportWorkHistories(String amrId, LocalDateTime from, LocalDateTime to, String taskType) {
        List<WorkHistoryDto> filtered = findFilteredWorkHistories(amrId, taskType, from, to, null);
        String csvBody = buildCsvRows(filtered);
        return csvBody.getBytes(StandardCharsets.UTF_8);
    }

    public String buildExportFileName(String workHistoryId) {
        return "work-history-" + workHistoryId + ".csv";
    }

    public String buildBulkExportFileName() {
        return "work-histories-export.csv";
    }

    private List<WorkHistoryDto> findFilteredWorkHistories(
            String amrId,
            String taskType,
            LocalDateTime from,
            LocalDateTime to,
            String result
    ) {
        return amrTaskRepository.findAll().stream()
                .map(this::toWorkHistoryDto)
                .filter(dto -> matchesAmrIdFilter(dto, amrId))
                .filter(dto -> matchesTaskTypeFilter(dto, taskType))
                .filter(dto -> matchesFromFilter(dto, from))
                .filter(dto -> matchesToFilter(dto, to))
                .filter(dto -> matchesResultFilter(dto, result))
                .sorted(Comparator.comparing(WorkHistoryDto::getStartTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private AmrTask findTaskOrThrow(String workHistoryId) {
        Integer taskId = WorkHistoryIdentifierHelper.parseWorkHistoryId(workHistoryId);
        return amrTaskRepository.findById(taskId)
                .orElseThrow(() -> new WorkHistoryNotFoundException(workHistoryId));
    }

    private WorkHistoryDto toWorkHistoryDto(AmrTask task) {
        WorkHistoryDto dto = new WorkHistoryDto();
        dto.setId(WorkHistoryIdentifierHelper.formatWorkHistoryId(task.getTaskId()));
        dto.setAmrId(task.getAmr() != null ? AmrIdentifierHelper.formatAmrId(task.getAmr().getAmrId()) : null);
        dto.setTaskType(task.getTaskType());
        dto.setStartTime(task.getPickTime());
        dto.setEndTime(task.getDropTime());
        dto.setFrom(task.getFromArea() != null ? task.getFromArea().getAreaName() : null);
        dto.setTo(task.getToArea() != null ? task.getToArea().getAreaName() : null);
        dto.setResult(mapTaskResult(task.getStatus()));
        return dto;
    }

    private String mapTaskResult(String status) {
        if (status == null) {
            return "unknown";
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("complete") || normalized.contains("success") || normalized.contains("완료")) {
            return "success";
        }
        if (normalized.contains("fail") || normalized.contains("error") || normalized.contains("cancel")) {
            return "failed";
        }
        if (normalized.contains("progress") || normalized.contains("진행")) {
            return "in_progress";
        }
        return normalized;
    }

    private boolean matchesAmrIdFilter(WorkHistoryDto dto, String amrId) {
        if (amrId == null || amrId.isBlank()) {
            return true;
        }
        return amrId.trim().equalsIgnoreCase(dto.getAmrId());
    }

    private boolean matchesTaskTypeFilter(WorkHistoryDto dto, String taskType) {
        if (taskType == null || taskType.isBlank()) {
            return true;
        }
        return taskType.trim().equalsIgnoreCase(dto.getTaskType());
    }

    private boolean matchesFromFilter(WorkHistoryDto dto, LocalDateTime from) {
        if (from == null || dto.getStartTime() == null) {
            return true;
        }
        return !dto.getStartTime().isBefore(from);
    }

    private boolean matchesToFilter(WorkHistoryDto dto, LocalDateTime to) {
        if (to == null || dto.getStartTime() == null) {
            return true;
        }
        return !dto.getStartTime().isAfter(to);
    }

    private boolean matchesResultFilter(WorkHistoryDto dto, String result) {
        if (result == null || result.isBlank()) {
            return true;
        }
        return result.trim().equalsIgnoreCase(dto.getResult());
    }

    private String buildCsvRows(List<WorkHistoryDto> workHistories) {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append(CSV_HEADER).append('\n');
        for (WorkHistoryDto workHistory : workHistories) {
            builder.append(escapeCsv(workHistory.getId())).append(',');
            builder.append(escapeCsv(workHistory.getAmrId())).append(',');
            builder.append(escapeCsv(workHistory.getTaskType())).append(',');
            builder.append(escapeCsv(formatTime(workHistory.getStartTime()))).append(',');
            builder.append(escapeCsv(formatTime(workHistory.getEndTime()))).append(',');
            builder.append(escapeCsv(workHistory.getFrom())).append(',');
            builder.append(escapeCsv(workHistory.getTo())).append(',');
            builder.append(escapeCsv(workHistory.getResult())).append('\n');
        }
        return builder.toString();
    }

    private String formatTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : CSV_TIME_FORMAT.format(dateTime);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
