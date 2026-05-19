package com.sfaas.amr_control_system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.demo")
public class DemoRecoveryProperties {

    /**
     * ERROR·EMERGENCY_STOP 진입 후 자동 복구까지 대기(초). 시연 기본 60.
     */
    private int recoverySeconds = 60;

    /**
     * 미해결 상태 스캔 주기(밀리초).
     */
    private long recoveryCheckIntervalMs = 10_000L;
}
