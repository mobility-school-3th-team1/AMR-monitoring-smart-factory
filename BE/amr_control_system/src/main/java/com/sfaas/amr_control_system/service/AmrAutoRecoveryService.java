package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.config.DemoSimulationProperties;
import com.sfaas.amr_control_system.entity.AmrStatusLog;
import com.sfaas.amr_control_system.repository.AmrStatusLogRepository;
import com.sfaas.amr_control_system.util.DashboardStatusNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 시연용 운행 자동 복구. FE {@code resume} 없이 ERROR·EMERGENCY_STOP을 IDLE로 되돌린다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AmrAutoRecoveryService {

    private final AmrStatusLogRepository amrStatusLogRepository;
    private final DemoSimulationProperties demoSimulationProperties;
    private final StreamNotificationService streamNotificationService;

    @Scheduled(fixedDelayString = "${app.demo.recovery-check-interval-ms:10000}")
    @Transactional
    public void recoverUnresolvedAmrStatuses() {
        LocalDateTime now = LocalDateTime.now();
        Duration recoveryDelay = Duration.ofSeconds(demoSimulationProperties.getRecoverySeconds());

        for (AmrStatusLog statusLog : findLatestStatusPerAmr()) {
            if (statusLog.getUpdatedAt() == null) {
                continue;
            }

            String normalizedStatus = DashboardStatusNormalizer.normalizeAmrStatus(statusLog.getStatus());
            if (!DashboardStatusNormalizer.isUnresolvedAmrError(
                    normalizedStatus,
                    statusLog.getFaultRecoveredAt(),
                    statusLog.getEmergencyResolvedAt())) {
                continue;
            }

            Duration elapsed = Duration.between(statusLog.getUpdatedAt(), now);
            if (elapsed.compareTo(recoveryDelay) < 0) {
                continue;
            }

            applyAutoRecovery(statusLog, normalizedStatus, now);
        }
    }

    private void applyAutoRecovery(AmrStatusLog statusLog, String normalizedStatus, LocalDateTime recoveredAt) {
        statusLog.setStatus(DashboardStatusNormalizer.STATUS_IDLE);
        statusLog.setUpdatedAt(recoveredAt);

        if (DashboardStatusNormalizer.STATUS_EMERGENCY_STOP.equals(normalizedStatus)) {
            statusLog.setEmergencyResolvedAt(recoveredAt);
        } else if (DashboardStatusNormalizer.STATUS_ERROR.equals(normalizedStatus)) {
            statusLog.setFaultRecoveredAt(recoveredAt);
            statusLog.setFaultCode(null);
            statusLog.setFaultMessage(null);
        }

        amrStatusLogRepository.save(statusLog);

        String amrLabel = statusLog.getAmr() != null
                ? AmrIdentifierHelper.formatAmrId(statusLog.getAmr().getAmrId())
                : "unknown";
        streamNotificationService.publishAmrStatusChangeAfterCommit(
                amrLabel,
                DashboardStatusNormalizer.STATUS_IDLE,
                null
        );
        log.info("Auto-recovered AMR {} from {} to IDLE", amrLabel, normalizedStatus);
    }

    private List<AmrStatusLog> findLatestStatusPerAmr() {
        return amrStatusLogRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(log -> log.getAmr() != null)
                .collect(Collectors.toMap(
                        log -> log.getAmr().getAmrId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .toList();
    }
}
