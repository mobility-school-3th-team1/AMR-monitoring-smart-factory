package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.dto.AlarmSummaryDto;
import com.sfaas.amr_control_system.dto.DashboardRecentLogDto;
import com.sfaas.amr_control_system.dto.DashboardRecentLogsResponseDto;
import com.sfaas.amr_control_system.dto.DashboardSummaryDto;
import com.sfaas.amr_control_system.dto.RecentAlarmsDto;
import com.sfaas.amr_control_system.entity.Alarm;
import com.sfaas.amr_control_system.entity.AmrStatusLog;
import com.sfaas.amr_control_system.entity.AmrTask;
import com.sfaas.amr_control_system.entity.WorkOrder;
import com.sfaas.amr_control_system.repository.AlarmRepository;
import com.sfaas.amr_control_system.repository.AmrStatusLogRepository;
import com.sfaas.amr_control_system.repository.AmrTaskRepository;
import com.sfaas.amr_control_system.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final int DEFAULT_ALARM_LIMIT = 10;

    private final AmrStatusLogRepository amrStatusLogRepository;
    private final AmrTaskRepository amrTaskRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AlarmRepository alarmRepository;

    public DashboardSummaryDto getSummary() {
        List<AmrStatusLog> latestStatusPerAmr = findLatestStatusPerAmr();

        int amrOperating = 0;
        int amrCharging = 0;
        int amrWaiting = 0;
        int batterySum = 0;
        int batteryCount = 0;

        for (AmrStatusLog statusLog : latestStatusPerAmr) {
            String normalizedStatus = DashboardStatusNormalizer.normalizeAmrStatus(statusLog.getStatus());
            switch (normalizedStatus) {
                case "operating" -> amrOperating++;
                case "charging" -> amrCharging++;
                case "waiting" -> amrWaiting++;
                default -> {
                    // error 등은 알람으로만 반영
                }
            }
            if (statusLog.getBatteryPct() != null) {
                batterySum += statusLog.getBatteryPct();
                batteryCount++;
            }
        }

        DashboardSummaryDto summary = new DashboardSummaryDto();
        summary.setProductionCount(calculateProductionCount());
        summary.setActiveAlarms(countActiveAlarms());
        summary.setAmrOperating(amrOperating);
        summary.setAmrCharging(amrCharging);
        summary.setAmrWaiting(amrWaiting);
        summary.setAvgBatteryPercent(batteryCount == 0 ? 0 : Math.round((float) batterySum / batteryCount));
        summary.setAverageTaskTimeMin(calculateAverageTaskTimeMinutes());
        return summary;
    }

    public RecentAlarmsDto getRecentAlarms(Integer limit) {
        int resolvedLimit = limit == null || limit < 1 ? DEFAULT_ALARM_LIMIT : limit;
        List<AlarmSummaryDto> alarms = alarmRepository.findByAcknowledgedFalseOrderByOccurredAtDesc().stream()
                .limit(resolvedLimit)
                .map(this::toAlarmSummaryDto)
                .toList();

        RecentAlarmsDto response = new RecentAlarmsDto();
        response.setData(alarms);
        return response;
    }

    public DashboardRecentLogsResponseDto getRecentLogs(Integer page, Integer limit) {
        int resolvedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int resolvedLimit = limit == null || limit < 1 ? DEFAULT_LIMIT : limit;

        Pageable pageable = PageRequest.of(resolvedPage - 1, resolvedLimit);
        Page<AmrStatusLog> statusLogPage = amrStatusLogRepository.findAllByOrderByUpdatedAtDesc(pageable);

        List<DashboardRecentLogDto> logs = statusLogPage.getContent().stream()
                .map(this::toRecentLogDto)
                .toList();

        DashboardRecentLogsResponseDto response = new DashboardRecentLogsResponseDto();
        response.setData(logs);
        response.setTotal((int) statusLogPage.getTotalElements());
        response.setPage(resolvedPage);
        response.setLimit(resolvedLimit);
        return response;
    }

    private List<AmrStatusLog> findLatestStatusPerAmr() {
        return amrStatusLogRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(log -> log.getAmr() != null)
                .collect(Collectors.toMap(
                        log -> log.getAmr().getAmrId(),
                        Function.identity(),
                        (existing, replacement) ->
                                existing.getUpdatedAt().isAfter(replacement.getUpdatedAt()) ? existing : replacement
                ))
                .values()
                .stream()
                .toList();
    }

    private int calculateProductionCount() {
        return workOrderRepository.findAll().stream()
                .filter(this::isProductionWorkOrder)
                .mapToInt(order -> order.getPlannedQty() == null ? 0 : order.getPlannedQty())
                .sum();
    }

    private boolean isProductionWorkOrder(WorkOrder workOrder) {
        if (workOrder.getStatus() == null) {
            return true;
        }
        String normalized = workOrder.getStatus().trim().toLowerCase(Locale.ROOT);
        return !normalized.contains("cancel") && !normalized.contains("취소");
    }

    private int countActiveAlarms() {
        return (int) alarmRepository.countByAcknowledgedFalse();
    }

    private AlarmSummaryDto toAlarmSummaryDto(Alarm alarm) {
        AlarmSummaryDto summary = new AlarmSummaryDto();
        summary.setId(AlarmIdentifierHelper.formatAlarmId(alarm.getAlarmId()));
        summary.setLevel(alarm.getLevel());
        summary.setMessage(alarm.getMessage());
        summary.setOccurredAt(alarm.getOccurredAt());
        return summary;
    }

    private double calculateAverageTaskTimeMinutes() {
        return amrTaskRepository.findByPickTimeIsNotNullAndDropTimeIsNotNull().stream()
                .mapToDouble(this::taskDurationMinutes)
                .average()
                .orElse(0.0);
    }

    private double taskDurationMinutes(AmrTask task) {
        Duration duration = Duration.between(task.getPickTime(), task.getDropTime());
        return duration.toMinutes();
    }

    private DashboardRecentLogDto toRecentLogDto(AmrStatusLog statusLog) {
        DashboardRecentLogDto log = new DashboardRecentLogDto();
        log.setId(String.format("log-%03d", statusLog.getAmrStatlogId()));
        log.setSourceType("amr");
        log.setSourceId(statusLog.getAmr() != null
                ? String.format("amr-%02d", statusLog.getAmr().getAmrId())
                : null);
        log.setLevel(mapLogLevel(statusLog.getStatus()));
        log.setMessage(buildLogMessage(statusLog));
        log.setOccurredAt(statusLog.getUpdatedAt());
        return log;
    }

    private String mapLogLevel(String amrStatus) {
        return switch (DashboardStatusNormalizer.normalizeAmrStatus(amrStatus)) {
            case "error" -> "error";
            case "charging" -> "info";
            case "operating" -> "info";
            default -> "info";
        };
    }

    private String buildLogMessage(AmrStatusLog statusLog) {
        String amrName = statusLog.getAmr() != null ? statusLog.getAmr().getAmrName() : "AMR";
        String areaName = statusLog.getArea() != null ? statusLog.getArea().getAreaName() : "unknown";
        return String.format("%s status=%s area=%s battery=%s%%",
                amrName,
                statusLog.getStatus(),
                areaName,
                statusLog.getBatteryPct() != null ? statusLog.getBatteryPct() : "-");
    }
}
