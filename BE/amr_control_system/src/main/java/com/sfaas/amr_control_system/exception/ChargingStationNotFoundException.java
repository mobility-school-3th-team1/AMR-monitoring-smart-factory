package com.sfaas.amr_control_system.exception;

public class ChargingStationNotFoundException extends RuntimeException {

    public ChargingStationNotFoundException(String stationId) {
        super("Charging station not found: " + stationId);
    }
}
