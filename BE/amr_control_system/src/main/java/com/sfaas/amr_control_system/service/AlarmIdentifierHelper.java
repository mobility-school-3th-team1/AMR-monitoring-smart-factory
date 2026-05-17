package com.sfaas.amr_control_system.service;

final class AlarmIdentifierHelper {

    private static final String ID_PREFIX = "alarm-";

    private AlarmIdentifierHelper() {
    }

    static String formatAlarmId(Long alarmId) {
        return String.format("%s%03d", ID_PREFIX, alarmId);
    }

    static Long parseAlarmId(String alarmId) {
        if (alarmId == null || alarmId.isBlank()) {
            throw new IllegalArgumentException("alarmId is required.");
        }
        String normalized = alarmId.trim().toLowerCase();
        if (!normalized.startsWith(ID_PREFIX)) {
            throw new IllegalArgumentException("Invalid alarmId format. Expected example: alarm-001");
        }
        try {
            return Long.parseLong(normalized.substring(ID_PREFIX.length()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid alarmId format. Expected example: alarm-001");
        }
    }
}
