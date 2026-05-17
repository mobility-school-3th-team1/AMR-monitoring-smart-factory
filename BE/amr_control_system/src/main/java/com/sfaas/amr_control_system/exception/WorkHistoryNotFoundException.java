package com.sfaas.amr_control_system.exception;

public class WorkHistoryNotFoundException extends RuntimeException {

    public WorkHistoryNotFoundException(String workHistoryId) {
        super("Work history not found: " + workHistoryId);
    }
}
