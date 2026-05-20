package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.entity.Alarm;
import com.sfaas.amr_control_system.entity.Amr;
import com.sfaas.amr_control_system.entity.AmrStatusLog;
import com.sfaas.amr_control_system.entity.AmrTask;
import com.sfaas.amr_control_system.exception.AmrNotFoundException;
import com.sfaas.amr_control_system.repository.AlarmRepository;
import com.sfaas.amr_control_system.repository.AmrRepository;
import com.sfaas.amr_control_system.repository.AmrStatusLogRepository;
import com.sfaas.amr_control_system.repository.AmrTaskRepository;
import com.sfaas.amr_control_system.util.DashboardStatusNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoScenarioService {

    private static final String TASK_STATUS_CANCELLED = "CANCELLED";
    private static final String DEMO_EMERGENCY_FAULT_CODE = "DEMO_E001";
    private static final int DEMO_LOW_BATTERY_PCT = 18;

    private final AmrRepository amrRepository;
    private final AmrStatusLogRepository amrStatusLogRepository;
    private final AmrTaskRepository amrTaskRepository;
    private final AlarmRepository alarmRepository;
    private final AmrDemoSimulationService amrDemoSimulationService;
    private final StreamNotificationService streamNotificationService;

    @Transactional
    public void triggerEmergencyError(String amrId) {
        Amr amr = findAmrOrThrow(amrId);
        LocalDateTime now = LocalDateTime.now();
        String formattedAmrId = AmrIdentifierHelper.formatAmrId(amr.getAmrId());

        cancelActiveTask(amr, now);

        AmrStatusLog previousLog = findLatestStatusLog(amr);
        AmrStatusLog statusLog = appendStatusSnapshot(previousLog, DashboardStatusNormalizer.STATUS_ERROR, log -> {
            log.setFaultCode(DEMO_EMERGENCY_FAULT_CODE);
            log.setFaultMessage(formattedAmrId + " 기능 고장 감지 (시연 비상 시나리오)");
            log.setFaultRecoveredAt(null);
            log.setEmergencyResolvedAt(null);
        });

        Alarm alarm = new Alarm();
        alarm.setSourceType("amr");
        alarm.setSourceId(formattedAmrId);
        alarm.setLevel("CRITICAL");
        alarm.setMessage(statusLog.getFaultMessage());
        alarm.setOccurredAt(now);
        alarm.setAcknowledged(false);
        alarmRepository.save(alarm);

        streamNotificationService.publishAmrStatusChangeAfterCommit(
                formattedAmrId,
                DashboardStatusNormalizer.STATUS_ERROR,
                statusLog.getFaultMessage()
        );

        log.info("Demo emergency ERROR applied to {}", formattedAmrId);
    }

    @Transactional
    public void triggerLowBatteryCharge(String amrId) {
        Amr amr = findAmrOrThrow(amrId);
        LocalDateTime now = LocalDateTime.now();

        cancelActiveTask(amr, now);

        AmrStatusLog previousLog = findLatestStatusLog(amr);
        AmrStatusLog statusLog = appendStatusSnapshot(previousLog, previousLog.getStatus(), log -> {
            log.setFaultCode(null);
            log.setFaultMessage(null);
            log.setFaultRecoveredAt(null);
            log.setEmergencyResolvedAt(null);
            log.setBatteryPct(DEMO_LOW_BATTERY_PCT);
        });

        amrDemoSimulationService.beginChargeApproach(statusLog, now);

        log.info("Demo low-battery charge approach for {}", AmrIdentifierHelper.formatAmrId(amr.getAmrId()));
    }

    private AmrStatusLog appendStatusSnapshot(
            AmrStatusLog previousLog,
            String status,
            Consumer<AmrStatusLog> customizer
    ) {
        AmrStatusLog nextLog = new AmrStatusLog();
        nextLog.setAmr(previousLog.getAmr());
        nextLog.setArea(previousLog.getArea());
        nextLog.setStatus(status);
        nextLog.setPosX(previousLog.getPosX());
        nextLog.setPosY(previousLog.getPosY());
        nextLog.setYaw(previousLog.getYaw());
        nextLog.setLoadWeight(previousLog.getLoadWeight());
        nextLog.setBatteryPct(previousLog.getBatteryPct());
        nextLog.setSohPct(previousLog.getSohPct());
        nextLog.setBatteryTemp(previousLog.getBatteryTemp());
        nextLog.setFaultCode(previousLog.getFaultCode());
        nextLog.setFaultMessage(previousLog.getFaultMessage());
        nextLog.setFaultRecoveredAt(previousLog.getFaultRecoveredAt());
        nextLog.setEmergencyResolvedAt(previousLog.getEmergencyResolvedAt());
        customizer.accept(nextLog);
        nextLog.setUpdatedAt(LocalDateTime.now());
        return amrStatusLogRepository.save(nextLog);
    }

    private AmrStatusLog findLatestStatusLog(Amr amr) {
        return amrStatusLogRepository.findFirstByAmr_AmrIdOrderByUpdatedAtDesc(amr.getAmrId())
                .orElseGet(() -> {
                    AmrStatusLog newLog = new AmrStatusLog();
                    newLog.setAmr(amr);
                    return newLog;
                });
    }

    private void cancelActiveTask(Amr amr, LocalDateTime cancelledAt) {
        amrTaskRepository.findFirstByAmr_AmrIdAndDropTimeIsNullOrderByPickTimeDesc(amr.getAmrId())
                .ifPresent(activeTask -> {
                    activeTask.setStatus(TASK_STATUS_CANCELLED);
                    activeTask.setDropTime(cancelledAt);
                    amrTaskRepository.save(activeTask);
                });
    }

    private Amr findAmrOrThrow(String amrId) {
        return amrRepository.findById(AmrIdentifierHelper.parseAmrId(amrId))
                .orElseThrow(() -> new AmrNotFoundException(amrId));
    }
}
