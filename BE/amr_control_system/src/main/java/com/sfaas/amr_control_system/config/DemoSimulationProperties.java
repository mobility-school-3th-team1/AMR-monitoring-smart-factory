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

    /** 충전 스테이션 맵 좌표(백분율 0~100, 원점 좌측 상단) */
    private int chargingPositionXPercent = 5;

    private int chargingPositionYPercent = 15;

    /** 충전소 도착 판정 허용 오차(%) */
    private double chargingArrivalTolerancePercent = 1.0;

    /** EN_ROUTE_CHARGING 시 충전소 방향 이동량(%/tick) */
    private double chargeApproachStepPercent = 2.0;

    /** 충전소 도착(CHARGING 전환) 후 실제 배터리 충전 시작까지 대기(초) */
    private int chargeStartDelaySeconds = 2;
}
