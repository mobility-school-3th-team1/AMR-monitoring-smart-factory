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

    /** 배터리 이하이면 작업 완료 후 충전 진입(%) */
    private int lowBatteryChargeThresholdPct = 20;

    /** 배터리 이하이면 STOPPED(%) */
    private int criticalStopBatteryThresholdPct = 5;

    /** 충전 스테이션 맵 좌표(백분율 0~100). 0.5 → 50 */
    private int chargingPositionXPercent = 50;

    private int chargingPositionYPercent = 80;
}
