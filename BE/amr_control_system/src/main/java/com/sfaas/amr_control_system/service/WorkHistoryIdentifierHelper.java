package com.sfaas.amr_control_system.service;

final class WorkHistoryIdentifierHelper {

    private static final String ID_PREFIX = "wh-";

    private WorkHistoryIdentifierHelper() {
    }

    static String formatWorkHistoryId(Long taskId) {
        return String.format("%s%03d", ID_PREFIX, taskId);
    }

    static Long parseWorkHistoryId(String workHistoryId) {
        if (workHistoryId == null || workHistoryId.isBlank()) {
            throw new IllegalArgumentException("workHistoryId is required.");
        }
        String normalized = workHistoryId.trim().toLowerCase();
        if (!normalized.startsWith(ID_PREFIX)) {
            throw new IllegalArgumentException("Invalid workHistoryId format. Expected example: wh-001");
        }
        try {
            return Long.parseLong(normalized.substring(ID_PREFIX.length()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid workHistoryId format. Expected example: wh-001");
        }
    }
}
