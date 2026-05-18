package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.dto.AnalyticsBatteryDto;
import com.sfaas.amr_control_system.dto.AnalyticsKpiDto;
import com.sfaas.amr_control_system.dto.AnalyticsWorkloadDto;
import com.sfaas.amr_control_system.entity.AmrChargeStation;
import com.sfaas.amr_control_system.entity.AmrStatusLog;
import com.sfaas.amr_control_system.entity.AmrTask;
import com.sfaas.amr_control_system.entity.WorkOrder;
import com.sfaas.amr_control_system.exception.ChargingStationNotFoundException;
import com.sfaas.amr_control_system.repository.AlarmRepository;
import com.sfaas.amr_control_system.repository.AmrChargeStationRepository;
import com.sfaas.amr_control_system.repository.AmrStatusLogRepository;
import com.sfaas.amr_control_system.repository.AmrTaskRepository;
import com.sfaas.amr_control_system.repository.WorkOrderRepository;
import com.sfaas.amr_control_system.util.DashboardStatusNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final int DEFAULT_RANGE_HOURS = 24;
    private static final String GROUP_BY_HOUR = "hour";
    private static final String GROUP_BY_DAY = "day";
    private static final String GROUP_BY_AMR = "amr";
    private static final String GROUP_BY_TASK_TYPE = "tasktype";

    private final AmrStatusLogRepository amrStatusLogRepository;
    private final AmrTaskRepository amrTaskRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AlarmRepository alarmRepository;
    private final AmrChargeStationRepository amrChargeStationRepository;

    public List<AnalyticsKpiDto> getKpis(LocalDateTime from, LocalDateTime to, String groupBy) {
        LocalDateTime rangeEnd = resolveTo(to);
        LocalDateTime rangeStart = resolveFrom(from, rangeEnd);
        String resolvedGroupBy = resolveTimeGroupBy(groupBy);

        List<LocalDateTime> bucketStarts = generateTimeBuckets(rangeStart, rangeEnd, resolvedGroupBy);
        List<AnalyticsKpiDto> result = new ArrayList<>();

        for (int index = 0; index < bucketStarts.size(); index++) {
            LocalDateTime bucketStart = bucketStarts.get(index);
            LocalDateTime bucketEnd = index + 1 < bucketStarts.size()
                    ? bucketStarts.get(index + 1).minusNanos(1)
                    : rangeEnd;

            List<AmrStatusLog> latestPerAmr = findLatestStatusPerAmrUpTo(bucketEnd, rangeStart);
            int amrOperating = 0;
            int amrCharging = 0;
            int amrWaiting = 0;
            int batterySum = 0;
            int batteryCount = 0;

            for (AmrStatusLog statusLog : latestPerAmr) {
                String normalizedStatus = DashboardStatusNormalizer.normalizeAmrStatus(statusLog.getStatus());
                switch (normalizedStatus) {
                    case "operating" -> amrOperating++;
                    case "charging" -> amrCharging++;
                    case "waiting" -> amrWaiting++;
                    default -> {
                    }
                }
                if (statusLog.getBatteryPct() != null) {
                    batterySum += statusLog.getBatteryPct();
                    batteryCount++;
                }
            }

            AnalyticsKpiDto kpi = new AnalyticsKpiDto();
            kpi.setTimestamp(bucketStart);
            kpi.setProductionCount(calculateProductionCount());
            kpi.setActiveAlarms(countActiveAlarmsInRange(bucketStart, bucketEnd));
            kpi.setAmrOperating(amrOperating);
            kpi.setAmrCharging(amrCharging);
            kpi.setAmrWaiting(amrWaiting);
            kpi.setAvgBatteryPercent(batteryCount == 0 ? 0 : Math.round((float) batterySum / batteryCount));
            kpi.setAverageTaskTimeMin(calculateAverageTaskTimeMinutes(bucketStart, bucketEnd));
            result.add(kpi);
        }

        return result;
    }

    public List<AnalyticsBatteryDto> getBattery(
            String amrId,
            String stationId,
            LocalDateTime from,
            LocalDateTime to,
            String groupBy
    ) {
        LocalDateTime rangeEnd = resolveTo(to);
        LocalDateTime rangeStart = resolveFrom(from, rangeEnd);
        String resolvedGroupBy = resolveTimeGroupBy(groupBy);
        Integer filterAmrId = parseOptionalAmrId(amrId);
        Integer filterAreaId = resolveStationAreaId(stationId);

        List<AmrStatusLog> logs = amrStatusLogRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(log -> log.getUpdatedAt() != null)
                .filter(log -> !log.getUpdatedAt().isBefore(rangeStart))
                .filter(log -> !log.getUpdatedAt().isAfter(rangeEnd))
                .filter(log -> filterAmrId == null || (log.getAmr() != null && filterAmrId.equals(log.getAmr().getAmrId())))
                .filter(log -> filterAreaId == null || (log.getArea() != null && filterAreaId.equals(log.getArea().getAreaId())))
                .filter(log -> log.getBatteryPct() != null)
                .toList();

        Map<String, List<AmrStatusLog>> grouped = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> bucketKey(log.getUpdatedAt(), resolvedGroupBy) + "|" + formatAmrId(log),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<AmrStatusLog> bucketLogs = entry.getValue();
                    AmrStatusLog sample = bucketLogs.get(0);
                    int averageBattery = (int) Math.round(bucketLogs.stream()
                            .mapToInt(AmrStatusLog::getBatteryPct)
                            .average()
                            .orElse(0.0));

                    AnalyticsBatteryDto dto = new AnalyticsBatteryDto();
                    dto.setTimestamp(parseBucketTimestamp(entry.getKey()));
                    dto.setAmrId(formatAmrId(sample));
                    dto.setBatteryPercent(averageBattery);
                    return dto;
                })
                .sorted(Comparator.comparing(AnalyticsBatteryDto::getTimestamp)
                        .thenComparing(AnalyticsBatteryDto::getAmrId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public List<AnalyticsWorkloadDto> getWorkload(LocalDateTime from, LocalDateTime to, String groupBy) {
        LocalDateTime rangeEnd = resolveTo(to);
        LocalDateTime rangeStart = resolveFrom(from, rangeEnd);
        String resolvedGroupBy = normalizeGroupBy(groupBy, GROUP_BY_AMR);

        List<AmrTask> tasks = amrTaskRepository.findAll().stream()
                .filter(task -> task.getPickTime() != null)
                .filter(task -> !task.getPickTime().isBefore(rangeStart))
                .filter(task -> !task.getPickTime().isAfter(rangeEnd))
                .toList();

        if (GROUP_BY_TASK_TYPE.equals(resolvedGroupBy)) {
            return tasks.stream()
                    .collect(Collectors.groupingBy(
                            task -> task.getTaskType() == null ? "unknown" : task.getTaskType(),
                            LinkedHashMap::new,
                            Collectors.counting()
                    ))
                    .entrySet().stream()
                    .map(entry -> {
                        AnalyticsWorkloadDto dto = new AnalyticsWorkloadDto();
                        dto.setTimestamp(rangeEnd);
                        dto.setTaskType(entry.getKey());
                        dto.setTaskCount(entry.getValue().intValue());
                        return dto;
                    })
                    .sorted(Comparator.comparing(AnalyticsWorkloadDto::getTaskType))
                    .toList();
        }

        if (GROUP_BY_HOUR.equals(resolvedGroupBy)) {
            return tasks.stream()
                    .collect(Collectors.groupingBy(
                            task -> bucketKey(task.getPickTime(), GROUP_BY_HOUR),
                            LinkedHashMap::new,
                            Collectors.counting()
                    ))
                    .entrySet().stream()
                    .map(entry -> {
                        AnalyticsWorkloadDto dto = new AnalyticsWorkloadDto();
                        dto.setTimestamp(parseBucketTimestamp(entry.getKey()));
                        dto.setTaskCount(entry.getValue().intValue());
                        return dto;
                    })
                    .sorted(Comparator.comparing(AnalyticsWorkloadDto::getTimestamp))
                    .toList();
        }

        return tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getAmr() != null
                                ? AmrIdentifierHelper.formatAmrId(task.getAmr().getAmrId())
                                : "unknown",
                        LinkedHashMap::new,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> {
                    AnalyticsWorkloadDto dto = new AnalyticsWorkloadDto();
                    dto.setTimestamp(rangeEnd);
                    dto.setAmrId(entry.getKey());
                    dto.setTaskCount(entry.getValue().intValue());
                    return dto;
                })
                .sorted(Comparator.comparing(AnalyticsWorkloadDto::getAmrId))
                .toList();
    }

    private LocalDateTime resolveTo(LocalDateTime to) {
        return to != null ? to : LocalDateTime.now();
    }

    private LocalDateTime resolveFrom(LocalDateTime from, LocalDateTime to) {
        if (from != null) {
            return from;
        }
        return to.minusHours(DEFAULT_RANGE_HOURS);
    }

    private String resolveTimeGroupBy(String groupBy) {
        return normalizeGroupBy(groupBy, GROUP_BY_HOUR);
    }

    private String normalizeGroupBy(String groupBy, String defaultValue) {
        if (groupBy == null || groupBy.isBlank()) {
            return defaultValue;
        }
        String normalized = groupBy.trim().toLowerCase(Locale.ROOT);
        if (GROUP_BY_DAY.equals(normalized) || GROUP_BY_HOUR.equals(normalized)
                || GROUP_BY_AMR.equals(normalized) || GROUP_BY_TASK_TYPE.equals(normalized)
                || "task_type".equals(normalized)) {
            if ("task_type".equals(normalized)) {
                return GROUP_BY_TASK_TYPE;
            }
            return normalized;
        }
        throw new IllegalArgumentException("Unsupported groupBy value: " + groupBy);
    }

    private List<LocalDateTime> generateTimeBuckets(LocalDateTime from, LocalDateTime to, String groupBy) {
        List<LocalDateTime> buckets = new ArrayList<>();
        if (GROUP_BY_DAY.equals(groupBy)) {
            LocalDateTime cursor = from.toLocalDate().atStartOfDay();
            while (!cursor.isAfter(to)) {
                buckets.add(cursor);
                cursor = cursor.plusDays(1);
            }
            return buckets;
        }

        LocalDateTime cursor = from.truncatedTo(ChronoUnit.HOURS);
        while (!cursor.isAfter(to)) {
            buckets.add(cursor);
            cursor = cursor.plusHours(1);
        }
        if (buckets.isEmpty()) {
            buckets.add(from.truncatedTo(ChronoUnit.HOURS));
        }
        return buckets;
    }

    private List<AmrStatusLog> findLatestStatusPerAmrUpTo(LocalDateTime upTo, LocalDateTime notBefore) {
        return amrStatusLogRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(log -> log.getAmr() != null)
                .filter(log -> log.getUpdatedAt() != null)
                .filter(log -> !log.getUpdatedAt().isAfter(upTo))
                .filter(log -> !log.getUpdatedAt().isBefore(notBefore))
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

    private int countActiveAlarmsInRange(LocalDateTime from, LocalDateTime to) {
        return (int) alarmRepository.findAll().stream()
                .filter(alarm -> alarm.getOccurredAt() != null)
                .filter(alarm -> !alarm.getOccurredAt().isBefore(from))
                .filter(alarm -> !alarm.getOccurredAt().isAfter(to))
                .filter(alarm -> !Boolean.TRUE.equals(alarm.getAcknowledged()))
                .count();
    }

    private double calculateAverageTaskTimeMinutes(LocalDateTime from, LocalDateTime to) {
        return amrTaskRepository.findByPickTimeIsNotNullAndDropTimeIsNotNull().stream()
                .filter(task -> task.getDropTime() != null)
                .filter(task -> !task.getDropTime().isBefore(from))
                .filter(task -> !task.getDropTime().isAfter(to))
                .mapToDouble(this::taskDurationMinutes)
                .average()
                .orElse(0.0);
    }

    private double taskDurationMinutes(AmrTask task) {
        Duration duration = Duration.between(task.getPickTime(), task.getDropTime());
        return duration.toMinutes();
    }

    private Integer parseOptionalAmrId(String amrId) {
        if (amrId == null || amrId.isBlank()) {
            return null;
        }
        return AmrIdentifierHelper.parseAmrId(amrId);
    }

    private Integer resolveStationAreaId(String stationId) {
        if (stationId == null || stationId.isBlank()) {
            return null;
        }
        Integer parsedStationId = ChargingStationIdentifierHelper.parseStationId(stationId);
        AmrChargeStation station = amrChargeStationRepository.findById(parsedStationId)
                .orElseThrow(() -> new ChargingStationNotFoundException(stationId));
        return station.getArea() != null ? station.getArea().getAreaId() : null;
    }

    private String bucketKey(LocalDateTime timestamp, String groupBy) {
        if (GROUP_BY_DAY.equals(groupBy)) {
            return timestamp.toLocalDate().atStartOfDay().toString();
        }
        return timestamp.truncatedTo(ChronoUnit.HOURS).toString();
    }

    private LocalDateTime parseBucketTimestamp(String bucketKey) {
        if (bucketKey.contains("|")) {
            bucketKey = bucketKey.substring(0, bucketKey.indexOf('|'));
        }
        return LocalDateTime.parse(bucketKey);
    }

    private String formatAmrId(AmrStatusLog statusLog) {
        if (statusLog.getAmr() == null) {
            return null;
        }
        return AmrIdentifierHelper.formatAmrId(statusLog.getAmr().getAmrId());
    }
}
