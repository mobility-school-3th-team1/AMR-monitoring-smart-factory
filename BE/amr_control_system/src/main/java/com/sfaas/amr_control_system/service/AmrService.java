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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfaas.amr_control_system.entity.Amr;
import com.sfaas.amr_control_system.entity.AmrCommand;
import com.sfaas.amr_control_system.entity.AmrStatusLog;
import com.sfaas.amr_control_system.entity.AmrTask;
import com.sfaas.amr_control_system.entity.Area;
import com.sfaas.amr_control_system.exception.AmrNotFoundException;
import com.sfaas.amr_control_system.exception.InvalidAmrCommandException;
import com.sfaas.amr_control_system.repository.AmrCommandRepository;
import com.sfaas.amr_control_system.repository.AmrRepository;
import com.sfaas.amr_control_system.repository.AmrStatusLogRepository;
import com.sfaas.amr_control_system.repository.AmrTaskRepository;
import com.sfaas.amr_control_system.util.DashboardStatusNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AmrService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final Set<String> SUPPORTED_COMMANDS = Set.of(
            "goto", "pause", "resume", "canceltask", "emergencystop"
    );
    /** 시연 시 DB에 반영하는 명령. goTo/pause/resume/cancelTask는 API만 허용, 실행은 400. */
    private static final String DEMO_EXECUTABLE_COMMAND = "emergencystop";
    private static final String COMMAND_STATUS_EXECUTED = "EXECUTED";
    private static final String TASK_STATUS_CANCELLED = "CANCELLED";
    private static final String EMPTY_JSON_PARAMS = "{}";
    private static final String SORT_UNRESOLVED_FIRST = "unresolvedfirst";
    private static final String INVALID_STATUS_QUERY_MESSAGE =
            "status must include one or more of: " + DashboardStatusNormalizer.SUPPORTED_AMR_QUERY_STATUS_VALUES;

    private final AmrRepository amrRepository;
    private final AmrStatusLogRepository amrStatusLogRepository;
    private final AmrTaskRepository amrTaskRepository;
    private final AmrCommandRepository amrCommandRepository;
    private final ObjectMapper objectMapper;

    public AmrListResponseDto listAmrs(
            Integer page,
            Integer limit,
            String status,
            Integer batteryMin,
            Integer batteryMax,
            String search,
            String sort
    ) {
        int resolvedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int resolvedLimit = limit == null || limit < 1 ? DEFAULT_LIMIT : limit;
        Set<String> wantedStatuses = parseStatusFilterTokens(status);

        List<AmrDto> filtered = amrRepository.findAll().stream()
                .map(amr -> {
                    AmrStatusLog latestLog = amrStatusLogRepository
                            .findFirstByAmr_AmrIdOrderByUpdatedAtDesc(amr.getAmrId())
                            .orElse(null);
                    return new AmrListRow(buildAmrDto(amr, latestLog), latestLog);
                })
                .filter(row -> matchesStatusFilter(row.dto(), wantedStatuses))
                .filter(row -> matchesBatteryFilter(row.dto(), batteryMin, batteryMax))
                .filter(row -> matchesSearchFilter(row.dto(), search))
                .sorted(amrListRowComparator(sort))
                .map(AmrListRow::dto)
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
        String commandType = resolveCommandType(request);
        if (!DEMO_EXECUTABLE_COMMAND.equals(commandType)) {
            throw new InvalidAmrCommandException(
                    "Command is not enabled in demo simulation. Executable: emergencyStop.");
        }

        String commandId = "cmd-" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime requestedAt = LocalDateTime.now();

        applyEmergencyStopStatus(amr, requestedAt);
        cancelActiveTasksForEmergencyStop(amr, requestedAt);
        persistAmrCommand(amr, commandId, commandType, request, requestedAt);

        AmrCommandResponseDto response = new AmrCommandResponseDto();
        response.setAccepted(true);
        response.setCommandId(commandId);
        response.setAmrId(AmrIdentifierHelper.formatAmrId(amr.getAmrId()));
        return response;
    }

    private void applyEmergencyStopStatus(Amr amr, LocalDateTime updatedAt) {
        AmrStatusLog statusLog = amrStatusLogRepository.findFirstByAmr_AmrIdOrderByUpdatedAtDesc(amr.getAmrId())
                .orElseGet(() -> {
                    AmrStatusLog newLog = new AmrStatusLog();
                    newLog.setAmr(amr);
                    return newLog;
                });

        statusLog.setStatus(DashboardStatusNormalizer.STATUS_EMERGENCY_STOP);
        statusLog.setFaultCode(null);
        statusLog.setFaultMessage(null);
        statusLog.setFaultRecoveredAt(null);
        statusLog.setEmergencyResolvedAt(null);
        statusLog.setUpdatedAt(updatedAt);
        amrStatusLogRepository.save(statusLog);
    }

    private void cancelActiveTasksForEmergencyStop(Amr amr, LocalDateTime cancelledAt) {
        amrTaskRepository.findFirstByAmr_AmrIdAndDropTimeIsNullOrderByPickTimeDesc(amr.getAmrId())
                .ifPresent(activeTask -> {
                    activeTask.setStatus(TASK_STATUS_CANCELLED);
                    activeTask.setDropTime(cancelledAt);
                    amrTaskRepository.save(activeTask);
                });
    }

    private void persistAmrCommand(
            Amr amr,
            String commandId,
            String commandType,
            AmrCommandRequestDto request,
            LocalDateTime requestedAt
    ) {
        AmrCommand command = new AmrCommand();
        command.setCommandId(commandId);
        command.setAmr(amr);
        command.setCommandType(toApiCommandName(commandType));
        command.setParams(serializeCommandParams(request));
        command.setAccepted(true);
        command.setStatus(COMMAND_STATUS_EXECUTED);
        command.setRequestedAt(requestedAt);
        command.setExecutedAt(requestedAt);
        amrCommandRepository.save(command);
    }

    private String toApiCommandName(String normalizedCommandType) {
        return switch (normalizedCommandType) {
            case "emergencystop" -> "emergencyStop";
            case "canceltask" -> "cancelTask";
            case "goto" -> "goTo";
            default -> normalizedCommandType;
        };
    }

    private String serializeCommandParams(AmrCommandRequestDto request) {
        if (request.getParams() == null || request.getParams().isEmpty()) {
            return EMPTY_JSON_PARAMS;
        }
        try {
            return objectMapper.writeValueAsString(request.getParams());
        } catch (JsonProcessingException exception) {
            throw new InvalidAmrCommandException("params must be valid JSON object.");
        }
    }

    private String resolveCommandType(AmrCommandRequestDto request) {
        validateCommand(request);
        return request.getCommand().trim().toLowerCase(Locale.ROOT);
    }

    private Amr findAmrOrThrow(String amrId) {
        Integer parsedId = AmrIdentifierHelper.parseAmrId(amrId);
        return amrRepository.findById(parsedId)
                .orElseThrow(() -> new AmrNotFoundException(amrId));
    }

    private AmrDto toAmrDto(Amr amr) {
        AmrStatusLog latestLog = amrStatusLogRepository.findFirstByAmr_AmrIdOrderByUpdatedAtDesc(amr.getAmrId())
                .orElse(null);
        return buildAmrDto(amr, latestLog);
    }

    private AmrDto buildAmrDto(Amr amr, AmrStatusLog latestLog) {
        AmrTask activeTask = amrTaskRepository.findFirstByAmr_AmrIdAndDropTimeIsNullOrderByPickTimeDesc(amr.getAmrId())
                .orElse(null);

        AmrDto dto = new AmrDto();
        dto.setId(AmrIdentifierHelper.formatAmrId(amr.getAmrId()));
        dto.setName(amr.getAmrName());
        dto.setStatus(latestLog == null
                ? DashboardStatusNormalizer.STATUS_IDLE
                : DashboardStatusNormalizer.normalizeAmrStatus(latestLog.getStatus()));
        if (latestLog != null) {
            dto.setFaultCode(latestLog.getFaultCode());
            dto.setFaultMessage(latestLog.getFaultMessage());
            dto.setLoadWeightKg(latestLog.getLoadWeight());
            dto.setSohPercent(latestLog.getSohPct());
        }
        dto.setBatteryPercent(latestLog != null ? latestLog.getBatteryPct() : null);
        dto.setTotalMileageKm(amr.getTotalMileage());
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

    private boolean matchesStatusFilter(AmrDto dto, Set<String> wantedStatuses) {
        if (wantedStatuses.isEmpty()) {
            return true;
        }
        return wantedStatuses.contains(dto.getStatus());
    }

    private Set<String> parseStatusFilterTokens(String statusQuery) {
        if (statusQuery == null || statusQuery.isBlank()) {
            return Set.of();
        }

        Set<String> wantedStatuses = Arrays.stream(statusQuery.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(DashboardStatusNormalizer::normalizeAmrQueryStatus)
                .collect(Collectors.toSet());

        if (wantedStatuses.isEmpty()) {
            throw new IllegalArgumentException(INVALID_STATUS_QUERY_MESSAGE);
        }

        return wantedStatuses;
    }

    private Comparator<AmrListRow> amrListRowComparator(String sort) {
        Comparator<AmrListRow> byName = Comparator.comparing(
                row -> row.dto().getName(),
                Comparator.nullsLast(String::compareToIgnoreCase)
        );
        if (sort == null || sort.isBlank()) {
            return byName;
        }
        if (!SORT_UNRESOLVED_FIRST.equals(sort.trim().toLowerCase(Locale.ROOT))) {
            return byName;
        }
        return Comparator
                .comparing((AmrListRow row) -> isUnresolvedAmrStatus(row.latestLog()))
                .reversed()
                .thenComparingInt(row -> emergencyStopBeforeErrorSortRank(row.dto().getStatus()))
                .thenComparing(row -> row.dto().getName(), Comparator.nullsLast(String::compareToIgnoreCase));
    }

    /**
     * 미해결 ERROR·EMERGENCY_STOP만 true. {@code docs/API 정의.md} §3 GET /amrs sort=unresolvedFirst.
     */
    private boolean isUnresolvedAmrStatus(AmrStatusLog latestLog) {
        if (latestLog == null) {
            return false;
        }
        String normalized = DashboardStatusNormalizer.normalizeAmrStatus(latestLog.getStatus());
        return DashboardStatusNormalizer.isUnresolvedAmrError(
                normalized,
                latestLog.getFaultRecoveredAt(),
                latestLog.getEmergencyResolvedAt()
        );
    }

    /** 동률 시 EMERGENCY_STOP이 ERROR보다 앞(작은 값). */
    private int emergencyStopBeforeErrorSortRank(String normalizedStatus) {
        if (DashboardStatusNormalizer.STATUS_EMERGENCY_STOP.equals(normalizedStatus)) {
            return 0;
        }
        if (DashboardStatusNormalizer.STATUS_ERROR.equals(normalizedStatus)) {
            return 1;
        }
        return 2;
    }

    private record AmrListRow(AmrDto dto, AmrStatusLog latestLog) {
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
