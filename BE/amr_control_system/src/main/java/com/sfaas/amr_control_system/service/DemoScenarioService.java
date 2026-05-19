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

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoScenarioService {

    private static final String TASK_STATUS_CANCELLED = "CANCELLED";
    private static final String TASK_STATUS_IN_PROGRESS = "IN_PROGRESS";
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

        AmrStatusLog statusLog = amrStatusLogRepository.findFirstByAmr_AmrIdOrderByUpdatedAtDesc(amr.getAmrId())
                .orElseGet(() -> {
                    AmrStatusLog newLog = new AmrStatusLog();
                    newLog.setAmr(amr);
                    return newLog;
                });

        statusLog.setStatus(DashboardStatusNormalizer.STATUS_ERROR);
        statusLog.setFaultCode(DEMO_EMERGENCY_FAULT_CODE);
        statusLog.setFaultMessage(formattedAmrId + " 기능 고장 감지 (시연 비상 시나리오)");
        statusLog.setFaultRecoveredAt(null);
        statusLog.setEmergencyResolvedAt(null);
        statusLog.setUpdatedAt(now);
        amrStatusLogRepository.save(statusLog);

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

        AmrStatusLog statusLog = amrStatusLogRepository.findFirstByAmr_AmrIdOrderByUpdatedAtDesc(amr.getAmrId())
                .orElseGet(() -> {
                    AmrStatusLog newLog = new AmrStatusLog();
                    newLog.setAmr(amr);
                    return newLog;
                });

        statusLog.setBatteryPct(DEMO_LOW_BATTERY_PCT);
        statusLog.setUpdatedAt(now);
        amrStatusLogRepository.save(statusLog);

        amrDemoSimulationService.beginChargeApproach(statusLog, now);

        log.info("Demo low-battery charge approach for {}", AmrIdentifierHelper.formatAmrId(amr.getAmrId()));
    }

    private void cancelActiveTask(Amr amr, LocalDateTime cancelledAt) {
        amrTaskRepository.findFirstByAmr_AmrIdAndDropTimeIsNullOrderByPickTimeDesc(amr.getAmrId())
                .ifPresent(activeTask -> {
                    if (TASK_STATUS_IN_PROGRESS.equals(activeTask.getStatus())) {
                        activeTask.setStatus(TASK_STATUS_CANCELLED);
                        activeTask.setDropTime(cancelledAt);
                        amrTaskRepository.save(activeTask);
                    }
                });
    }

    private Amr findAmrOrThrow(String amrId) {
        return amrRepository.findById(AmrIdentifierHelper.parseAmrId(amrId))
                .orElseThrow(() -> new AmrNotFoundException(amrId));
    }
}
