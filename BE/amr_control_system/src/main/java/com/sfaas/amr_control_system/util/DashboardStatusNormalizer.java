package com.sfaas.amr_control_system.util;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

public final class DashboardStatusNormalizer {

    public static final String STATUS_OPERATING = "OPERATING";
    public static final String STATUS_IDLE = "IDLE";
    public static final String STATUS_CHARGING = "CHARGING";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_EMERGENCY_STOP = "EMERGENCY_STOP";
    public static final String SUPPORTED_AMR_QUERY_STATUS_VALUES =
            "OPERATING, IDLE, CHARGING, ERROR, EMERGENCY_STOP";
    private static final Set<String> SUPPORTED_AMR_QUERY_STATUSES = Set.of(
            STATUS_OPERATING,
            STATUS_IDLE,
            STATUS_CHARGING,
            STATUS_ERROR,
            STATUS_EMERGENCY_STOP
    );
    private static final String INVALID_AMR_QUERY_STATUS_MESSAGE =
            "status value must be one of: " + SUPPORTED_AMR_QUERY_STATUS_VALUES;

    private DashboardStatusNormalizer() {
    }

    /**
     * DB·시드의 혼용 표기를 API 설계 enum(대문자)으로 통일한다.
     */
    public static String normalizeAmrStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_IDLE;
        }

        String trimmed = status.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);

        if (STATUS_OPERATING.equals(upper) || "RUNNING".equals(upper)) {
            return STATUS_OPERATING;
        }
        if (STATUS_IDLE.equals(upper) || "WAITING".equals(upper)) {
            return STATUS_IDLE;
        }
        if (STATUS_CHARGING.equals(upper)) {
            return STATUS_CHARGING;
        }
        if (STATUS_ERROR.equals(upper) || "FAULT".equals(upper)) {
            return STATUS_ERROR;
        }
        if (STATUS_EMERGENCY_STOP.equals(upper)
                || "EMERGENCYSTOP".equals(upper.replace("_", ""))
                || "STOPPED".equals(upper)) {
            return STATUS_EMERGENCY_STOP;
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.contains("비상") || lower.contains("emergency")) {
            return STATUS_EMERGENCY_STOP;
        }
        if (lower.contains("충전") || lower.contains("charging")) {
            return STATUS_CHARGING;
        }
        if (lower.contains("운영") || lower.contains("operating") || lower.contains("running")) {
            return STATUS_OPERATING;
        }
        if (lower.contains("오류") || lower.contains("error") || lower.contains("fault")) {
            return STATUS_ERROR;
        }
        if (lower.contains("대기") || lower.contains("idle") || lower.contains("waiting")) {
            return STATUS_IDLE;
        }

        return STATUS_IDLE;
    }

    public static String normalizeAmrQueryStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(INVALID_AMR_QUERY_STATUS_MESSAGE);
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (SUPPORTED_AMR_QUERY_STATUSES.contains(normalized)) {
            return normalized;
        }

        throw new IllegalArgumentException(INVALID_AMR_QUERY_STATUS_MESSAGE);
    }

    public static boolean isErrorStatus(String normalizedStatus) {
        return STATUS_ERROR.equals(normalizedStatus) || STATUS_EMERGENCY_STOP.equals(normalizedStatus);
    }

    /**
     * {@code docs/API 정의.md} §2 amrErrorUnresolved 집계 규칙.
     */
    public static boolean isUnresolvedAmrError(
            String normalizedStatus,
            LocalDateTime faultRecoveredAt,
            LocalDateTime emergencyResolvedAt) {
        if (STATUS_ERROR.equals(normalizedStatus)) {
            return faultRecoveredAt == null;
        }
        if (STATUS_EMERGENCY_STOP.equals(normalizedStatus)) {
            return emergencyResolvedAt == null;
        }
        return false;
    }

    public static boolean isCongestedStation(String stationStatus) {
        if (stationStatus == null) {
            return false;
        }
        String normalized = stationStatus.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("혼잡")
                || normalized.contains("congest")
                || normalized.contains("full");
    }
}
