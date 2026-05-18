package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.dto.AlarmAckRequestDto;
import com.sfaas.amr_control_system.dto.AlarmAckResponseDto;
import com.sfaas.amr_control_system.dto.AlarmCreateRequestDto;
import com.sfaas.amr_control_system.dto.AlarmDto;
import com.sfaas.amr_control_system.dto.AlarmListResponseDto;
import com.sfaas.amr_control_system.entity.Alarm;
import com.sfaas.amr_control_system.exception.AlarmNotFoundException;
import com.sfaas.amr_control_system.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final Set<String> SUPPORTED_LEVELS = Set.of("info", "warning", "critical");
    private static final Sort OCCURRED_AT_DESC = Sort.by(Sort.Order.desc("occurredAt"));

    private final AlarmRepository alarmRepository;

    public AlarmListResponseDto listAlarms(
            String level,
            Boolean acknowledged,
            String sourceType,
            String sourceId,
            LocalDateTime from,
            LocalDateTime to,
            Integer page,
            Integer limit
    ) {
        int resolvedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int resolvedLimit = limit == null || limit < 1 ? DEFAULT_LIMIT : limit;

        Specification<Alarm> specification = AlarmSpecification.withFilters(
                level, acknowledged, sourceType, sourceId, from, to
        );
        Pageable pageable = PageRequest.of(resolvedPage - 1, resolvedLimit, OCCURRED_AT_DESC);
        Page<Alarm> alarmPage = alarmRepository.findAll(specification, pageable);

        AlarmListResponseDto response = new AlarmListResponseDto();
        response.setData(alarmPage.getContent().stream().map(this::toAlarmDto).toList());
        response.setTotal((int) alarmPage.getTotalElements());
        return response;
    }

    public AlarmDto getAlarm(String alarmId) {
        return toAlarmDto(findAlarmOrThrow(alarmId));
    }

    @Transactional
    public AlarmAckResponseDto acknowledgeAlarm(String alarmId, AlarmAckRequestDto request, String username) {
        Alarm alarm = findAlarmOrThrow(alarmId);
        if (Boolean.TRUE.equals(alarm.getAcknowledged())) {
            throw new IllegalArgumentException("Alarm is already acknowledged.");
        }

        LocalDateTime acknowledgedAt = LocalDateTime.now();
        alarm.setAcknowledged(true);
        alarm.setAcknowledgedAt(acknowledgedAt);
        alarmRepository.save(alarm);

        AlarmAckResponseDto response = new AlarmAckResponseDto();
        response.setAcknowledged(true);
        response.setAckBy(username);
        response.setAckAt(acknowledgedAt);
        return response;
    }

    @Transactional
    public AlarmDto createAlarm(AlarmCreateRequestDto request) {
        validateCreateRequest(request);

        Alarm alarm = new Alarm();
        alarm.setSourceType(request.getSourceType().trim());
        alarm.setSourceId(request.getSourceId() != null ? request.getSourceId().trim() : null);
        alarm.setLevel(normalizeLevel(request.getLevel()));
        alarm.setMessage(request.getMessage().trim());
        alarm.setOccurredAt(LocalDateTime.now());
        alarm.setAcknowledged(false);

        return toAlarmDto(alarmRepository.save(alarm));
    }

    private void validateCreateRequest(AlarmCreateRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        if (request.getSourceType() == null || request.getSourceType().isBlank()) {
            throw new IllegalArgumentException("sourceType is required.");
        }
        if (request.getLevel() == null || request.getLevel().isBlank()) {
            throw new IllegalArgumentException("level is required.");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("message is required.");
        }
        normalizeLevel(request.getLevel());
    }

    private String normalizeLevel(String level) {
        String normalized = level.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_LEVELS.contains(normalized)) {
            throw new IllegalArgumentException("level must be one of: info, warning, critical");
        }
        return normalized;
    }

    private Alarm findAlarmOrThrow(String alarmId) {
        Long numericId = AlarmIdentifierHelper.parseAlarmId(alarmId);
        return alarmRepository.findById(numericId)
                .orElseThrow(() -> new AlarmNotFoundException(alarmId));
    }

    private AlarmDto toAlarmDto(Alarm alarm) {
        AlarmDto dto = new AlarmDto();
        dto.setId(AlarmIdentifierHelper.formatAlarmId(alarm.getAlarmId()));
        dto.setSourceType(alarm.getSourceType());
        dto.setSourceId(alarm.getSourceId());
        dto.setLevel(alarm.getLevel());
        dto.setMessage(alarm.getMessage());
        dto.setOccurredAt(alarm.getOccurredAt());
        dto.setAcknowledged(alarm.getAcknowledged());
        return dto;
    }
}
