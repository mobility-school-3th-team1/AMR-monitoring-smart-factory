package com.sfaas.amr_control_system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.demo")
public class DemoSimulationProperties {

    private int recoverySeconds = 60;

    private long recoveryCheckIntervalMs = 10_000L;

    private int taskDurationSeconds = 20;

    private long taskAssignIntervalMs = 8_000L;

    private int chargeRatePctPerSec = 5;

    private int idleDischargePct = 1;

    private int idleDischargeIntervalSec = 5;

    private int operatingDischargePct = 1;

    private int batteryFullPct = 100;

    private int primaryChargeStationId = 1;
}
