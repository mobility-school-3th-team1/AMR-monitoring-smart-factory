package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.dto.AmrCommandRequestDto;
import com.sfaas.amr_control_system.dto.AmrCommandResponseDto;
import com.sfaas.amr_control_system.dto.AmrDto;
import com.sfaas.amr_control_system.dto.AmrListResponseDto;
import com.sfaas.amr_control_system.dto.AmrPathDto;
import com.sfaas.amr_control_system.dto.AmrStatusDto;
import com.sfaas.amr_control_system.dto.AmrStatusHistoryDto;
import com.sfaas.amr_control_system.dto.PathPointDto;
import com.sfaas.amr_control_system.dto.PositionDto;
import com.sfaas.amr_control_system.entity.Amr;
import com.sfaas.amr_control_system.entity.AmrStatusLog;
import com.sfaas.amr_control_system.entity.AmrTask;
import com.sfaas.amr_control_system.entity.Area;
import com.sfaas.amr_control_system.exception.AmrNotFoundException;
import com.sfaas.amr_control_system.exception.InvalidAmrCommandException;
import com.sfaas.amr_control_system.repository.AmrRepository;
import com.sfaas.amr_control_system.repository.AmrStatusLogRepository;
import com.sfaas.amr_control_system.repository.AmrTaskRepository;
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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AmrService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final Set<String> SUPPORTED_COMMANDS = Set.of(
            "goto", "pause", "resume", "canceltask", "emergencystop"
    );

    private final AmrRepository amrRepository;
    private final AmrStatusLogRepository amrStatusLogRepository;
    private final AmrTaskRepository amrTaskRepository;

    public AmrListResponseDto listAmrs(
            Integer page,
            Integer limit,
            String status,
            Integer batteryMin,
            Integer batteryMax,
            String search
    ) {
        int resolvedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int resolvedLimit = limit == null || limit < 1 ? DEFAULT_LIMIT : limit;

        List<AmrDto> filtered = amrRepository.findAll().stream()
                .map(this::toAmrDto)
                .filter(dto -> matchesStatusFilter(dto, status))
                .filter(dto -> matchesBatteryFilter(dto, batteryMin, batteryMax))
                .filter(dto -> matchesSearchFilter(dto, search))
                .sorted(Comparator.comparing(AmrDto::getName))
                .toList();

        int fromIndex = Math.min((resolvedPage - 1) * resolvedLimit, filtered.size());
        int toIndex = Math.min(fromIndex + resolvedLimit, filtered.size());
        List<AmrDto> pageData = filtered.subList(fromIndex, toIndex);

        AmrListResponseDto response = new AmrListResponseDto();
        response.setData(pageData);
        response.setTotal(filtered.size());
        response.setPage(resolvedPage);
        response.setLimit(resolvedLimit);
        return response;
    }

    public AmrDto getAmr(String amrId) {
        Amr amr = findAmrOrThrow(amrId);
        return toAmrDto(amr);
    }

    public AmrStatusHistoryDto getStatusHistory(
            String amrId,
            LocalDateTime from,
            LocalDateTime to,
            String groupBy
    ) {
        Amr amr = findAmrOrThrow(amrId);
        LocalDateTime resolvedTo = to == null ? LocalDateTime.now() : to;
        LocalDateTime resolvedFrom = from == null ? resolvedTo.minusHours(24) : from;

        List<AmrStatusLog> logs = amrStatusLogRepository.findByAmr_AmrIdAndUpdatedAtBetweenOrderByUpdatedAtAsc(
                amr.getAmrId(),
                resolvedFrom,
                resolvedTo
        );

        List<AmrStatusDto> history = groupStatusLogs(logs, groupBy).stream()
                .map(log -> toAmrStatusDto(amr, log))
                .toList();

        AmrStatusHistoryDto response = new AmrStatusHistoryDto();
        response.setData(history);
        return response;
    }

    public AmrPathDto getPath(String amrId, LocalDateTime from, LocalDateTime to) {
        Amr amr = findAmrOrThrow(amrId);
        LocalDateTime resolvedTo = to == null ? LocalDateTime.now() : to;
        LocalDateTime resolvedFrom = from == null ? resolvedTo.minusHours(24) : from;

        List<PathPointDto> path = amrStatusLogRepository
                .findByAmr_AmrIdAndUpdatedAtBetweenOrderByUpdatedAtAsc(amr.getAmrId(), resolvedFrom, resolvedTo)
                .stream()
                .map(this::toPathPointDto)
                .toList();

        AmrPathDto response = new AmrPathDto();
        response.setAmrId(AmrIdentifierHelper.formatAmrId(amr.getAmrId()));
        response.setPath(path);
        return response;
    }

    @Transactional
    public AmrCommandResponseDto sendCommand(String amrId, AmrCommandRequestDto request) {
        Amr amr = findAmrOrThrow(amrId);
        validateCommand(request);

        AmrCommandResponseDto response = new AmrCommandResponseDto();
        response.setAccepted(true);
        response.setCommandId("cmd-" + UUID.randomUUID().toString().substring(0, 8));
        response.setAmrId(AmrIdentifierHelper.formatAmrId(amr.getAmrId()));
        return response;
    }

    private Amr findAmrOrThrow(String amrId) {
        Integer parsedId = AmrIdentifierHelper.parseAmrId(amrId);
        return amrRepository.findById(parsedId)
                .orElseThrow(() -> new AmrNotFoundException(amrId));
    }

    private AmrDto toAmrDto(Amr amr) {
        AmrStatusLog latestLog = amrStatusLogRepository.findFirstByAmr_AmrIdOrderByUpdatedAtDesc(amr.getAmrId())
                .orElse(null);
        AmrTask activeTask = amrTaskRepository.findFirstByAmr_AmrIdAndDropTimeIsNullOrderByPickTimeDesc(amr.getAmrId())
                .orElse(null);

        AmrDto dto = new AmrDto();
        dto.setId(AmrIdentifierHelper.formatAmrId(amr.getAmrId()));
        dto.setName(amr.getAmrName());
        dto.setStatus(latestLog == null ? "waiting" : DashboardStatusNormalizer.normalizeAmrStatus(latestLog.getStatus()));
        dto.setBatteryPercent(latestLog != null ? latestLog.getBatteryPct() : null);
        dto.setPosition(latestLog == null ? null : toPositionDto(latestLog.getArea(), latestLog.getPosX(), latestLog.getPosY()));
        dto.setDestination(activeTask == null ? null : toPositionDto(activeTask.getToArea(), null, null));
        dto.setCurrentTask(resolveCurrentTaskLabel(activeTask));
        dto.setLastSeenAt(latestLog != null ? latestLog.getUpdatedAt() : null);
        return dto;
    }

    private AmrStatusDto toAmrStatusDto(Amr amr, AmrStatusLog log) {
        AmrStatusDto dto = new AmrStatusDto();
        dto.setAmrId(AmrIdentifierHelper.formatAmrId(amr.getAmrId()));
        dto.setStatus(DashboardStatusNormalizer.normalizeAmrStatus(log.getStatus()));
        dto.setPosition(toPositionDto(log.getArea(), log.getPosX(), log.getPosY()));
        dto.setBatteryPercent(log.getBatteryPct());
        dto.setTimestamp(log.getUpdatedAt());
        return dto;
    }

    private PathPointDto toPathPointDto(AmrStatusLog log) {
        PathPointDto point = new PathPointDto();
        point.setTime(log.getUpdatedAt());
        point.setX(log.getPosX() == null ? null : log.getPosX().doubleValue());
        point.setY(log.getPosY() == null ? null : log.getPosY().doubleValue());
        point.setZone(log.getArea() != null ? log.getArea().getAreaName() : null);
        return point;
    }

    private PositionDto toPositionDto(Area area, Integer posX, Integer posY) {
        if (area == null && posX == null && posY == null) {
            return null;
        }
        PositionDto position = new PositionDto();
        position.setZone(area != null ? area.getAreaName() : null);
        position.setX(posX == null ? null : posX.doubleValue());
        position.setY(posY == null ? null : posY.doubleValue());
        return position;
    }

    private String resolveCurrentTaskLabel(AmrTask task) {
        if (task == null) {
            return null;
        }
        if (task.getTaskType() == null) {
            return "작업 수행 중";
        }
        return switch (task.getTaskType().toLowerCase(Locale.ROOT)) {
            case "transport" -> "부품 운반";
            case "charging" -> "충전 이동";
            default -> task.getTaskType();
        };
    }

    private boolean matchesStatusFilter(AmrDto dto, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        return status.trim().equalsIgnoreCase(dto.getStatus());
    }

    private boolean matchesBatteryFilter(AmrDto dto, Integer batteryMin, Integer batteryMax) {
        if (dto.getBatteryPercent() == null) {
            return batteryMin == null && batteryMax == null;
        }
        if (batteryMin != null && dto.getBatteryPercent() < batteryMin) {
            return false;
        }
        if (batteryMax != null && dto.getBatteryPercent() > batteryMax) {
            return false;
        }
        return true;
    }

    private boolean matchesSearchFilter(AmrDto dto, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String keyword = search.trim().toLowerCase(Locale.ROOT);
        return (dto.getName() != null && dto.getName().toLowerCase(Locale.ROOT).contains(keyword))
                || (dto.getId() != null && dto.getId().toLowerCase(Locale.ROOT).contains(keyword));
    }

    private List<AmrStatusLog> groupStatusLogs(List<AmrStatusLog> logs, String groupBy) {
        if (groupBy == null || groupBy.isBlank()) {
            return logs;
        }

        ChronoUnit unit = switch (groupBy.trim().toLowerCase(Locale.ROOT)) {
            case "minute" -> ChronoUnit.MINUTES;
            case "hour" -> ChronoUnit.HOURS;
            case "day" -> ChronoUnit.DAYS;
            default -> throw new IllegalArgumentException("groupBy must be one of: minute, hour, day");
        };

        Map<LocalDateTime, AmrStatusLog> bucketLatest = new LinkedHashMap<>();
        for (AmrStatusLog log : logs) {
            LocalDateTime bucket = log.getUpdatedAt().truncatedTo(unit);
            bucketLatest.put(bucket, log);
        }
        return new ArrayList<>(bucketLatest.values());
    }

    private void validateCommand(AmrCommandRequestDto request) {
        if (request == null || request.getCommand() == null || request.getCommand().isBlank()) {
            throw new InvalidAmrCommandException("command is required.");
        }
        String normalized = request.getCommand().trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_COMMANDS.contains(normalized)) {
            throw new InvalidAmrCommandException(
                    "Unsupported command. Supported: goTo, pause, resume, cancelTask, emergencyStop");
        }
    }
}
