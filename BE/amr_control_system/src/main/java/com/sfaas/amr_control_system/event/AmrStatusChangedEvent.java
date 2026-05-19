package com.sfaas.amr_control_system.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AmrStatusChangedEvent extends ApplicationEvent {

    private final String amrId;
    private final String status;
    private final String faultCode;

    public AmrStatusChangedEvent(Object source, String amrId, String status, String faultCode) {
        super(source);
        this.amrId = amrId;
        this.status = status;
        this.faultCode = faultCode;
    }
}
