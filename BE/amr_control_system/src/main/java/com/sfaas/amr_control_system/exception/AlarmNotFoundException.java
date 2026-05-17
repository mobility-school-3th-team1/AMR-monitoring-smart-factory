package com.sfaas.amr_control_system.exception;

public class AlarmNotFoundException extends RuntimeException {

    public AlarmNotFoundException(String alarmId) {
        super("Alarm not found: " + alarmId);
    }
}
