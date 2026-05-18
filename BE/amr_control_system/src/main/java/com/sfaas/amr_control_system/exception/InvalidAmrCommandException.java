package com.sfaas.amr_control_system.exception;

public class InvalidAmrCommandException extends RuntimeException {

    public InvalidAmrCommandException(String message) {
        super(message);
    }
}
