package com.sfaas.amr_control_system.service;

final class DashboardStatusNormalizer {

    private DashboardStatusNormalizer() {
    }

    static String normalizeAmrStatus(String status) {
        if (status == null) {
            return "waiting";
        }
        String normalized = status.trim().toLowerCase();
        if (normalized.contains("충전") || normalized.contains("charging")) {
            return "charging";
        }
        if (normalized.contains("운영") || normalized.contains("operating") || normalized.contains("running")) {
            return "operating";
        }
        if (normalized.contains("오류") || normalized.contains("error") || normalized.contains("fault")) {
            return "error";
        }
        return "waiting";
    }

    static boolean isCongestedStation(String stationStatus) {
        if (stationStatus == null) {
            return false;
        }
        String normalized = stationStatus.trim().toLowerCase();
        return normalized.contains("혼잡")
                || normalized.contains("congest")
                || normalized.contains("full");
    }
}
