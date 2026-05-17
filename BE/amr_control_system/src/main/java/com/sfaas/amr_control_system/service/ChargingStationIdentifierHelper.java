package com.sfaas.amr_control_system.service;

final class ChargingStationIdentifierHelper {

    private static final String ID_PREFIX = "station-";

    private ChargingStationIdentifierHelper() {
    }

    static String formatStationId(Integer stationId) {
        return ID_PREFIX + stationId;
    }

    static Integer parseStationId(String stationId) {
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException("stationId is required.");
        }
        String normalized = stationId.trim().toLowerCase();
        if (!normalized.startsWith(ID_PREFIX)) {
            throw new IllegalArgumentException("Invalid stationId format. Expected example: station-1");
        }
        try {
            return Integer.parseInt(normalized.substring(ID_PREFIX.length()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid stationId format. Expected example: station-1");
        }
    }
}
