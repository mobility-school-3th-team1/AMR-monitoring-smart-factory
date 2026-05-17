package com.sfaas.amr_control_system.service;

final class AmrIdentifierHelper {

    private static final String ID_PREFIX = "amr-";

    private AmrIdentifierHelper() {
    }

    static String formatAmrId(Integer amrId) {
        return String.format("%s%02d", ID_PREFIX, amrId);
    }

    static Integer parseAmrId(String amrId) {
        if (amrId == null || amrId.isBlank()) {
            throw new IllegalArgumentException("amrId is required.");
        }
        String normalized = amrId.trim().toLowerCase();
        if (!normalized.startsWith(ID_PREFIX)) {
            throw new IllegalArgumentException("Invalid amrId format. Expected example: amr-01");
        }
        try {
            return Integer.parseInt(normalized.substring(ID_PREFIX.length()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid amrId format. Expected example: amr-01");
        }
    }
}
