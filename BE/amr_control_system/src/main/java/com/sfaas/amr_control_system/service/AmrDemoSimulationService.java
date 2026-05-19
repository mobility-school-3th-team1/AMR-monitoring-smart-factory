package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.config.DemoSimulationProperties;
import com.sfaas.amr_control_system.entity.Amr;
import com.sfaas.amr_control_system.entity.AmrChargeStation;
import com.sfaas.amr_control_system.entity.AmrChargingSession;
import com.sfaas.amr_control_system.entity.AmrStatusLog;
import com.sfaas.amr_control_system.entity.AmrTask;
import com.sfaas.amr_control_system.entity.Area;
import com.sfaas.amr_control_system.repository.AmrChargeStationRepository;
import com.sfaas.amr_control_system.repository.AmrChargingSessionRepository;
import com.sfaas.amr_control_system.repository.AmrStatusLogRepository;
import com.sfaas.amr_control_system.repository.AmrTaskRepository;
import com.sfaas.amr_control_system.repository.AreaRepository;
import com.sfaas.amr_control_system.util.DashboardStatusNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 시연용 AMR 운행·배터리·작업·충전 시뮬레이션.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AmrDemoSimulationService {

    private static final String TASK_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String TASK_STATUS_COMPLETED = "COMPLETED";
    private static final List<String> DEMO_TASK_TYPES = List.of("TRANSPORT", "PICK", "DELIVER");

    private final DemoSimulationProperties demoSimulationProperties;
    private final AmrStatusLogRepository amrStatusLogRepository;
    private final AmrTaskRepository amrTaskRepository;
    private final AmrChargingSessionRepository amrChargingSessionRepository;
    private final AmrChargeStationRepository amrChargeStationRepository;
    private final AreaRepository areaRepository;
    private final Random random = new Random();
    private final AtomicInteger simulationTickCounter = new AtomicInteger(0);

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void runSimulationTick() {
        LocalDateTime now = LocalDateTime.now();
        int tick = simulationTickCounter.incrementAndGet();

        completeOverdueTasks(now);

        for (AmrStatusLog statusLog : findLatestStatusPerAmr()) {
            if (statusLog.getAmr() == null) {
                continue;
            }

            String normalizedStatus = DashboardStatusNormalizer.normalizeAmrStatus(statusLog.getStatus());
            if (DashboardStatusNormalizer.isUnresolvedAmrError(
                    normalizedStatus,
                    statusLog.getFaultRecoveredAt(),
                    statusLog.getEmergencyResolvedAt())) {
                continue;
            }

            if (DashboardStatusNormalizer.STATUS_STOPPED.equals(normalizedStatus)) {
                continue;
            }

            switch (normalizedStatus) {
                case DashboardStatusNormalizer.STATUS_CHARGING -> applyChargingTick(statusLog, now);
                case DashboardStatusNormalizer.STATUS_OPERATING -> {
                    applyBatteryDelta(statusLog, -demoSimulationProperties.getOperatingDischargePct(), now);
                    applyCriticalStopIfNeeded(statusLog, now);
                }
                case DashboardStatusNormalizer.STATUS_IDLE -> {
                    if (tick % demoSimulationProperties.getIdleDischargeIntervalSec() == 0) {
                        applyBatteryDelta(statusLog, -demoSimulationProperties.getIdleDischargePct(), now);
                        applyCriticalStopIfNeeded(statusLog, now);
                    }
                    tryEnterChargingFromIdle(statusLog, now);
                }
                default -> {
                    // no battery simulation for other states
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.demo.task-assign-interval-ms:8000}")
    @Transactional
    public void tryAssignRandomIdleTask() {
        List<AmrStatusLog> idleCandidates = findLatestStatusPerAmr().stream()
                .filter(log -> log.getAmr() != null)
                .filter(log -> DashboardStatusNormalizer.STATUS_IDLE.equals(
                        DashboardStatusNormalizer.normalizeAmrStatus(log.getStatus())))
                .filter(log -> !isLowBattery(log))
                .filter(log -> amrTaskRepository.findFirstByAmr_AmrIdAndDropTimeIsNullOrderByPickTimeDesc(log.getAmr().getAmrId()).isEmpty())
                .toList();

        if (idleCandidates.isEmpty()) {
            return;
        }

        AmrStatusLog selectedLog = idleCandidates.get(random.nextInt(idleCandidates.size()));
        assignTaskToAmr(selectedLog, LocalDateTime.now());
    }

    private void completeOverdueTasks(LocalDateTime now) {
        Duration taskDuration = Duration.ofSeconds(demoSimulationProperties.getTaskDurationSeconds());

        for (AmrTask task : amrTaskRepository.findByStatusAndDropTimeIsNull(TASK_STATUS_IN_PROGRESS)) {
            if (task.getPickTime() == null || task.getAmr() == null) {
                continue;
            }
            if (Duration.between(task.getPickTime(), now).compareTo(taskDuration) < 0) {
                continue;
            }
            completeTask(task, now);
        }
    }

    private void assignTaskToAmr(AmrStatusLog statusLog, LocalDateTime now) {
        Amr amr = statusLog.getAmr();
        List<Area> areas = areaRepository.findAll();
        if (areas.size() < 2) {
            return;
        }

        Area fromArea = areas.get(random.nextInt(areas.size()));
        Area toArea = areas.get(random.nextInt(areas.size()));

        AmrTask task = new AmrTask();
        task.setAmr(amr);
        task.setTaskType(DEMO_TASK_TYPES.get(random.nextInt(DEMO_TASK_TYPES.size())));
        task.setFromArea(fromArea);
        task.setToArea(toArea);
        task.setStatus(TASK_STATUS_IN_PROGRESS);
        task.setPickTime(now);
        amrTaskRepository.save(task);

        statusLog.setStatus(DashboardStatusNormalizer.STATUS_OPERATING);
        statusLog.setArea(fromArea);
        statusLog.setUpdatedAt(now);
        amrStatusLogRepository.save(statusLog);

        log.debug("Assigned demo task {} to AMR {}", task.getTaskId(), AmrIdentifierHelper.formatAmrId(amr.getAmrId()));
    }

    private void completeTask(AmrTask task, LocalDateTime now) {
        task.setStatus(TASK_STATUS_COMPLETED);
        task.setDropTime(now);
        amrTaskRepository.save(task);

        Amr amr = task.getAmr();
        amrStatusLogRepository.findFirstByAmr_AmrIdOrderByUpdatedAtDesc(amr.getAmrId())
                .ifPresent(statusLog -> {
                    if (isLowBattery(statusLog)) {
                        enterChargingState(statusLog, now);
                    } else {
                        statusLog.setStatus(DashboardStatusNormalizer.STATUS_IDLE);
                        statusLog.setArea(task.getToArea());
                        statusLog.setUpdatedAt(now);
                        amrStatusLogRepository.save(statusLog);
                    }
                });

        log.debug("Completed demo task {} for AMR {}", task.getTaskId(), AmrIdentifierHelper.formatAmrId(amr.getAmrId()));
    }

    private void tryEnterChargingFromIdle(AmrStatusLog statusLog, LocalDateTime now) {
        if (!isLowBattery(statusLog)) {
            return;
        }
        if (amrTaskRepository.findFirstByAmr_AmrIdAndDropTimeIsNullOrderByPickTimeDesc(statusLog.getAmr().getAmrId()).isPresent()) {
            return;
        }
        enterChargingState(statusLog, now);
    }

    private void enterChargingState(AmrStatusLog statusLog, LocalDateTime now) {
        statusLog.setStatus(DashboardStatusNormalizer.STATUS_CHARGING);
        statusLog.setPosX(demoSimulationProperties.getChargingPositionXPercent());
        statusLog.setPosY(demoSimulationProperties.getChargingPositionYPercent());
        statusLog.setUpdatedAt(now);
        amrStatusLogRepository.save(statusLog);

        Amr amr = statusLog.getAmr();
        boolean hasActiveSession = amrChargingSessionRepository.findByEndTimeIsNull().stream()
                .anyMatch(session -> session.getAmr() != null
                        && session.getAmr().getAmrId().equals(amr.getAmrId()));

        if (!hasActiveSession) {
            AmrChargeStation station = amrChargeStationRepository
                    .findById(demoSimulationProperties.getPrimaryChargeStationId())
                    .orElse(null);
            if (station == null) {
                return;
            }
            AmrChargingSession session = new AmrChargingSession();
            session.setAmr(amr);
            session.setStation(station);
            session.setSessionStatus("CHARGING");
            session.setStartTime(now);
            amrChargingSessionRepository.save(session);
        }

        log.info("AMR {} entered CHARGING at map ({}, {})",
                AmrIdentifierHelper.formatAmrId(amr.getAmrId()),
                statusLog.getPosX(),
                statusLog.getPosY());
    }

    private void applyChargingTick(AmrStatusLog statusLog, LocalDateTime now) {
        int currentBattery = batteryPercent(statusLog);
        int nextBattery = Math.min(
                demoSimulationProperties.getBatteryFullPct(),
                currentBattery + demoSimulationProperties.getChargeRatePctPerSec()
        );
        statusLog.setBatteryPct(nextBattery);
        statusLog.setPosX(demoSimulationProperties.getChargingPositionXPercent());
        statusLog.setPosY(demoSimulationProperties.getChargingPositionYPercent());
        statusLog.setUpdatedAt(now);
        amrStatusLogRepository.save(statusLog);

        if (nextBattery >= demoSimulationProperties.getBatteryFullPct()) {
            finishCharging(statusLog.getAmr(), statusLog, now);
        }
    }

    private void finishCharging(Amr amr, AmrStatusLog statusLog, LocalDateTime now) {
        statusLog.setStatus(DashboardStatusNormalizer.STATUS_IDLE);
        statusLog.setUpdatedAt(now);
        amrStatusLogRepository.save(statusLog);

        amrChargingSessionRepository.findByEndTimeIsNull().stream()
                .filter(session -> session.getAmr() != null && session.getAmr().getAmrId().equals(amr.getAmrId()))
                .forEach(session -> {
                    session.setEndTime(now);
                    session.setSessionStatus("COMPLETED");
                    amrChargingSessionRepository.save(session);
                });
    }

    private void applyBatteryDelta(AmrStatusLog statusLog, int delta, LocalDateTime now) {
        int currentBattery = batteryPercent(statusLog);
        int nextBattery = Math.max(0, currentBattery + delta);
        statusLog.setBatteryPct(nextBattery);
        statusLog.setUpdatedAt(now);
        amrStatusLogRepository.save(statusLog);
    }

    private void applyCriticalStopIfNeeded(AmrStatusLog statusLog, LocalDateTime now) {
        if (batteryPercent(statusLog) > demoSimulationProperties.getCriticalStopBatteryThresholdPct()) {
            return;
        }
        statusLog.setStatus(DashboardStatusNormalizer.STATUS_STOPPED);
        statusLog.setUpdatedAt(now);
        amrStatusLogRepository.save(statusLog);
        log.warn("AMR {} entered STOPPED (battery {}%)",
                AmrIdentifierHelper.formatAmrId(statusLog.getAmr().getAmrId()),
                statusLog.getBatteryPct());
    }

    private boolean isLowBattery(AmrStatusLog statusLog) {
        return batteryPercent(statusLog) <= demoSimulationProperties.getLowBatteryChargeThresholdPct();
    }

    private int batteryPercent(AmrStatusLog statusLog) {
        return statusLog.getBatteryPct() == null ? 0 : statusLog.getBatteryPct();
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
