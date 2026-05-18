package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.dto.ChargingForecastDto;
import com.sfaas.amr_control_system.dto.ChargingHistoryItemDto;
import com.sfaas.amr_control_system.dto.ChargingHistoryResponseDto;
import com.sfaas.amr_control_system.dto.ChargingQueueItemDto;
import com.sfaas.amr_control_system.dto.ChargingQueueResponseDto;
import com.sfaas.amr_control_system.dto.ChargingStationDto;
import com.sfaas.amr_control_system.dto.ChargingStationListDto;
import com.sfaas.amr_control_system.dto.ForecastBucketDto;
import com.sfaas.amr_control_system.entity.Amr;
import com.sfaas.amr_control_system.entity.AmrChargeStation;
import com.sfaas.amr_control_system.entity.AmrChargingSession;
import com.sfaas.amr_control_system.entity.AmrStatusLog;
import com.sfaas.amr_control_system.exception.ChargingStationNotFoundException;
import com.sfaas.amr_control_system.repository.AmrChargeStationRepository;
import com.sfaas.amr_control_system.repository.AmrChargingSessionRepository;
import com.sfaas.amr_control_system.repository.AmrStatusLogRepository;
import com.sfaas.amr_control_system.util.DashboardStatusNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChargingService {

    private static final int DEFAULT_STATION_CAPACITY = 4;
    private static final int DEFAULT_FULL_CHARGE_MINUTES = 45;

    private final AmrChargeStationRepository amrChargeStationRepository;
    private final AmrChargingSessionRepository amrChargingSessionRepository;
    private final AmrStatusLogRepository amrStatusLogRepository;

    public ChargingStationListDto listStations() {
        List<ChargingStationDto> stations = amrChargeStationRepository.findAll().stream()
                .map(this::toStationDto)
                .sorted(Comparator.comparing(ChargingStationDto::getName))
                .toList();

        ChargingStationListDto response = new ChargingStationListDto();
        response.setData(stations);
        return response;
    }

    public ChargingStationDto getStation(String stationId) {
        AmrChargeStation station = findStationOrThrow(stationId);
        return toStationDto(station);
    }

    public ChargingQueueResponseDto getQueue(String stationId, String status) {
        List<ChargingQueueItemDto> queueItems = amrChargingSessionRepository.findByEndTimeIsNull().stream()
                .filter(session -> matchesStationFilter(session, stationId))
                .filter(session -> matchesQueueStatusFilter(session, status))
                .map(this::toQueueItemDto)
                .sorted(Comparator.comparing(ChargingQueueItemDto::getQueuedAt))
                .toList();

        ChargingQueueResponseDto response = new ChargingQueueResponseDto();
        response.setData(queueItems);
        return response;
    }

    public ChargingForecastDto getForecast(
            String stationId,
            LocalDateTime from,
            LocalDateTime to,
            String groupBy
    ) {
        if (stationId != null && !stationId.isBlank()) {
            findStationOrThrow(stationId);
        }

        List<AmrChargingSession> activeSessions = amrChargingSessionRepository.findByEndTimeIsNull().stream()
                .filter(session -> matchesStationFilter(session, stationId))
                .toList();

        Map<String, Integer> bucketCounts = new LinkedHashMap<>();
        bucketCounts.put("0-30m", 0);
        bucketCounts.put("30-60m", 0);
        bucketCounts.put("60m+", 0);

        for (AmrChargingSession session : activeSessions) {
            int minutesToFull = estimateMinutesToFullCharge(session);
            String bucket = resolveForecastBucket(minutesToFull);
            bucketCounts.put(bucket, bucketCounts.get(bucket) + 1);
        }

        List<ForecastBucketDto> buckets = bucketCounts.entrySet().stream()
                .map(entry -> {
                    ForecastBucketDto bucket = new ForecastBucketDto();
                    bucket.setBucket(entry.getKey());
                    bucket.setCount(entry.getValue());
                    return bucket;
                })
                .toList();

        ChargingForecastDto response = new ChargingForecastDto();
        response.setData(buckets);
        return response;
    }

    public ChargingHistoryResponseDto getHistory(
            String amrId,
            String stationId,
            LocalDateTime from,
            LocalDateTime to,
            String groupBy
    ) {
        LocalDateTime resolvedTo = to == null ? LocalDateTime.now() : to;
        LocalDateTime resolvedFrom = from == null ? resolvedTo.minusDays(7) : from;

        List<ChargingHistoryItemDto> history = amrChargingSessionRepository.findByStartTimeBetween(resolvedFrom, resolvedTo)
                .stream()
                .filter(session -> session.getEndTime() != null)
                .filter(session -> matchesAmrFilter(session, amrId))
                .filter(session -> matchesStationFilter(session, stationId))
                .map(this::toHistoryItemDto)
                .sorted(Comparator.comparing(ChargingHistoryItemDto::getStartTime).reversed())
                .toList();

        if (groupBy != null && !groupBy.isBlank()) {
            history = aggregateHistory(history, groupBy);
        }

        ChargingHistoryResponseDto response = new ChargingHistoryResponseDto();
        response.setData(history);
        response.setTotal(history.size());
        return response;
    }

    private AmrChargeStation findStationOrThrow(String stationId) {
        Integer parsedId = ChargingStationIdentifierHelper.parseStationId(stationId);
        return amrChargeStationRepository.findById(parsedId)
                .orElseThrow(() -> new ChargingStationNotFoundException(stationId));
    }

    private ChargingStationDto toStationDto(AmrChargeStation station) {
        List<AmrChargingSession> activeSessions = amrChargingSessionRepository.findByStation_StationId(station.getStationId())
                .stream()
                .filter(session -> session.getEndTime() == null)
                .toList();

        int occupiedCount = activeSessions.size();
        int averageBattery = calculateAverageBattery(activeSessions);
        LocalDateTime estimatedFullChargeAt = estimateStationFullChargeAt(activeSessions);

        ChargingStationDto dto = new ChargingStationDto();
        dto.setId(ChargingStationIdentifierHelper.formatStationId(station.getStationId()));
        dto.setName(station.getStationName());
        dto.setLocation(station.getArea() != null ? station.getArea().getAreaName() : null);
        dto.setStatus(mapStationStatus(station.getStationStatus()));
        dto.setCapacity(DEFAULT_STATION_CAPACITY);
        dto.setOccupiedCount(occupiedCount);
        dto.setAverageBatteryPercent(averageBattery);
        dto.setEstimatedFullChargeAt(estimatedFullChargeAt);
        return dto;
    }

    private ChargingQueueItemDto toQueueItemDto(AmrChargingSession session) {
        Amr amr = session.getAmr();
        AmrChargeStation station = session.getStation();
        Integer batteryPercent = resolveBatteryPercent(amr);

        ChargingQueueItemDto item = new ChargingQueueItemDto();
        item.setId("queue-" + session.getChargingSessionId());
        item.setAmrId(amr != null ? AmrIdentifierHelper.formatAmrId(amr.getAmrId()) : null);
        item.setAmrName(amr != null ? amr.getAmrName() : null);
        item.setStationId(station != null ? ChargingStationIdentifierHelper.formatStationId(station.getStationId()) : null);
        item.setStationName(station != null ? station.getStationName() : null);
        item.setStatus(normalizeQueueStatus(session.getSessionStatus()));
        item.setBatteryPercent(batteryPercent);
        item.setQueuedAt(session.getStartTime());
        return item;
    }

    private ChargingHistoryItemDto toHistoryItemDto(AmrChargingSession session) {
        Amr amr = session.getAmr();
        AmrChargeStation station = session.getStation();

        ChargingHistoryItemDto item = new ChargingHistoryItemDto();
        item.setId("session-" + session.getChargingSessionId());
        item.setAmrId(amr != null ? AmrIdentifierHelper.formatAmrId(amr.getAmrId()) : null);
        item.setStationId(station != null ? ChargingStationIdentifierHelper.formatStationId(station.getStationId()) : null);
        item.setStationName(station != null ? station.getStationName() : null);
        item.setStartTime(session.getStartTime());
        item.setEndTime(session.getEndTime());
        item.setStartBatteryPercent(resolveBatteryAtTime(amr, session.getStartTime(), 30));
        item.setEndBatteryPercent(resolveBatteryAtTime(amr, session.getEndTime(), 90));
        item.setSessionStatus(session.getSessionStatus());
        return item;
    }

    private int calculateAverageBattery(List<AmrChargingSession> activeSessions) {
        List<Integer> batteries = activeSessions.stream()
                .map(session -> resolveBatteryPercent(session.getAmr()))
                .filter(battery -> battery != null)
                .toList();
        if (batteries.isEmpty()) {
            return 0;
        }
        return (int) Math.round(batteries.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private LocalDateTime estimateStationFullChargeAt(List<AmrChargingSession> activeSessions) {
        if (activeSessions.isEmpty()) {
            return LocalDateTime.now().plusMinutes(DEFAULT_FULL_CHARGE_MINUTES);
        }
        int maxMinutes = activeSessions.stream()
                .mapToInt(this::estimateMinutesToFullCharge)
                .max()
                .orElse(DEFAULT_FULL_CHARGE_MINUTES);
        return LocalDateTime.now().plusMinutes(maxMinutes);
    }

    private int estimateMinutesToFullCharge(AmrChargingSession session) {
        Integer batteryPercent = resolveBatteryPercent(session.getAmr());
        if (batteryPercent == null) {
            return DEFAULT_FULL_CHARGE_MINUTES;
        }
        int remainingPercent = Math.max(0, 100 - batteryPercent);
        return Math.max(5, (int) Math.ceil(remainingPercent * 0.45));
    }

    private String resolveForecastBucket(int minutesToFull) {
        if (minutesToFull <= 30) {
            return "0-30m";
        }
        if (minutesToFull <= 60) {
            return "30-60m";
        }
        return "60m+";
    }

    private List<ChargingHistoryItemDto> aggregateHistory(List<ChargingHistoryItemDto> history, String groupBy) {
        ChronoUnit unit = switch (groupBy.trim().toLowerCase(Locale.ROOT)) {
            case "hour" -> ChronoUnit.HOURS;
            case "day" -> ChronoUnit.DAYS;
            default -> throw new IllegalArgumentException("groupBy must be one of: hour, day");
        };

        Map<LocalDateTime, List<ChargingHistoryItemDto>> grouped = history.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getStartTime().truncatedTo(unit),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ChargingHistoryItemDto> aggregated = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<ChargingHistoryItemDto>> entry : grouped.entrySet()) {
            List<ChargingHistoryItemDto> bucketItems = entry.getValue();
            ChargingHistoryItemDto first = bucketItems.get(0);

            ChargingHistoryItemDto summary = new ChargingHistoryItemDto();
            summary.setId("history-" + entry.getKey());
            summary.setAmrId(first.getAmrId());
            summary.setStationId(first.getStationId());
            summary.setStationName(first.getStationName());
            summary.setStartTime(entry.getKey());
            summary.setEndTime(bucketItems.stream()
                    .map(ChargingHistoryItemDto::getEndTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(null));
            summary.setStartBatteryPercent((int) bucketItems.stream()
                    .filter(item -> item.getStartBatteryPercent() != null)
                    .mapToInt(ChargingHistoryItemDto::getStartBatteryPercent)
                    .average()
                    .orElse(0));
            summary.setEndBatteryPercent((int) bucketItems.stream()
                    .filter(item -> item.getEndBatteryPercent() != null)
                    .mapToInt(ChargingHistoryItemDto::getEndBatteryPercent)
                    .average()
                    .orElse(0));
            summary.setSessionStatus("aggregated");
            aggregated.add(summary);
        }
        return aggregated;
    }

    private String mapStationStatus(String stationStatus) {
        if (DashboardStatusNormalizer.isCongestedStation(stationStatus)) {
            return "congested";
        }
        return "normal";
    }

    private String normalizeQueueStatus(String sessionStatus) {
        if (sessionStatus == null) {
            return "waiting";
        }
        String normalized = sessionStatus.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("charg") || normalized.contains("충전")) {
            return "charging";
        }
        return "waiting";
    }

    private boolean matchesStationFilter(AmrChargingSession session, String stationId) {
        if (stationId == null || stationId.isBlank()) {
            return true;
        }
        if (session.getStation() == null) {
            return false;
        }
        return ChargingStationIdentifierHelper.formatStationId(session.getStation().getStationId())
                .equalsIgnoreCase(stationId.trim());
    }

    private boolean matchesAmrFilter(AmrChargingSession session, String amrId) {
        if (amrId == null || amrId.isBlank()) {
            return true;
        }
        if (session.getAmr() == null) {
            return false;
        }
        return AmrIdentifierHelper.formatAmrId(session.getAmr().getAmrId()).equalsIgnoreCase(amrId.trim());
    }

    private boolean matchesQueueStatusFilter(AmrChargingSession session, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        return normalizeQueueStatus(session.getSessionStatus()).equalsIgnoreCase(status.trim());
    }

    private Integer resolveBatteryPercent(Amr amr) {
        if (amr == null) {
            return null;
        }
        return amrStatusLogRepository.findFirstByAmr_AmrIdOrderByUpdatedAtDesc(amr.getAmrId())
                .map(AmrStatusLog::getBatteryPct)
                .orElse(null);
    }

    private Integer resolveBatteryAtTime(Amr amr, LocalDateTime time, int fallback) {
        if (amr == null || time == null) {
            return fallback;
        }
        return amrStatusLogRepository.findByAmr_AmrIdOrderByUpdatedAtDesc(amr.getAmrId()).stream()
                .filter(log -> !log.getUpdatedAt().isAfter(time))
                .findFirst()
                .map(AmrStatusLog::getBatteryPct)
                .orElse(fallback);
    }
}
