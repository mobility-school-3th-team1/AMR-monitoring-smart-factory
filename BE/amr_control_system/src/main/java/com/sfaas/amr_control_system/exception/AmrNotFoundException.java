package com.sfaas.amr_control_system.exception;

public class AmrNotFoundException extends RuntimeException {

    public AmrNotFoundException(String amrId) {
        super("AMR not found: " + amrId);
    }
}
