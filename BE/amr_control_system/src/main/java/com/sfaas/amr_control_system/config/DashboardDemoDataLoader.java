package com.sfaas.amr_control_system.config;

import com.sfaas.amr_control_system.entity.Alarm;
import com.sfaas.amr_control_system.entity.Amr;
import com.sfaas.amr_control_system.entity.AmrChargeStation;
import com.sfaas.amr_control_system.entity.AmrChargingSession;
import com.sfaas.amr_control_system.entity.AmrStatusLog;
import com.sfaas.amr_control_system.entity.AmrTask;
import com.sfaas.amr_control_system.entity.Area;
import com.sfaas.amr_control_system.entity.Site;
import com.sfaas.amr_control_system.entity.WorkOrder;
import com.sfaas.amr_control_system.repository.AlarmRepository;
import com.sfaas.amr_control_system.repository.AmrChargeStationRepository;
import com.sfaas.amr_control_system.repository.AmrChargingSessionRepository;
import com.sfaas.amr_control_system.repository.AmrRepository;
import com.sfaas.amr_control_system.repository.AmrStatusLogRepository;
import com.sfaas.amr_control_system.repository.AmrTaskRepository;
import com.sfaas.amr_control_system.repository.AreaRepository;
import com.sfaas.amr_control_system.repository.SiteRepository;
import com.sfaas.amr_control_system.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * H2 개발용 시드. {@code DB/init.sql} 물리 스키마·샘플 값과 동일한 ID·테이블 구조를 사용한다.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DashboardDemoDataLoader implements CommandLineRunner {

    private final AmrRepository amrRepository;
    private final SiteRepository siteRepository;
    private final AreaRepository areaRepository;
    private final AmrStatusLogRepository amrStatusLogRepository;
    private final AmrChargeStationRepository amrChargeStationRepository;
    private final AmrChargingSessionRepository amrChargingSessionRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AmrTaskRepository amrTaskRepository;
    private final AlarmRepository alarmRepository;

    @Override
    public void run(String... args) {
        if (amrRepository.count() > 0) {
            return;
        }

        log.info("Seeding H2 demo data (physical schema aligned with DB/init.sql).");

        Site site = new Site();
        site.setSiteId("SITE_BSA_01");
        site.setSiteName("BSA 제조 공장");
        siteRepository.save(site);

        Area loadArea = createArea(site, "AREA_LOAD_LC", "Lower Case 로딩 구역", "STORAGE",
                18, 25, 30, 60, 50, 150, 0, 20);
        Area assemble01 = createArea(site, "AREA_ASSEMBLE_01", "조립 구역 1", "PRODUCTION",
                18, 25, 30, 40, 10, 30, 0, 10);
        Area assemble02 = createArea(site, "AREA_ASSEMBLE_02", "조립 구역 2", "PRODUCTION",
                18, 22, 30, 50, 10, 30, 0, 10);
        Area outArea = createArea(site, "AREA_OUT_BSA", "BSA 출고 구역", "STORAGE",
                18, 22, 30, 50, 50, 150, 0, 10);
        areaRepository.saveAll(List.of(loadArea, assemble01, assemble02, outArea));

        List<Amr> amrs = List.of(
                createAmr(1, "AMR_BSA_01", 1736.2),
                createAmr(2, "AMR_BSA_02", 1538.1),
                createAmr(3, "AMR_BSA_03", 1645.8),
                createAmr(4, "AMR_BSA_04", 1592.4),
                createAmr(5, "AMR_BSA_05", 1721.7)
        );
        amrRepository.saveAll(amrs);

        LocalDateTime now = LocalDateTime.now();
        amrStatusLogRepository.saveAll(List.of(
                createStatusLog(amrs.get(0), assemble01, "OPERATING", 120, 450, 90, 50, 85, 98, 35.5f, now.minusMinutes(2)),
                createStatusLog(amrs.get(1), assemble02, "IDLE", 50, 200, 0, 0, 95, 99, 30.0f, now.minusMinutes(5)),
                createStatusLog(amrs.get(2), loadArea, "CHARGING", 300, 150, 180, 0, 20, 95, 28.5f, now.minusMinutes(10)),
                createStatusLog(amrs.get(3), assemble01, "ERROR", 130, 65, 0, 0, 20, 90, 31.0f, now.minusMinutes(3))
        ));

        AmrChargeStation station1 = createStation(1, loadArea, "충전소_입고", "AVAILABLE");
        AmrChargeStation station2 = createStation(2, loadArea, "충전소_입고", "OCCUPIED");
        AmrChargeStation station3 = createStation(3, loadArea, "충전소_입고", "OCCUPIED");
        amrChargeStationRepository.saveAll(List.of(station1, station2, station3));

        amrChargingSessionRepository.saveAll(List.of(
                createActiveSession(amrs.get(2), station2, "CHARGING", now.minusMinutes(25)),
                createActiveSession(amrs.get(1), station3, "WAITING", now.minusMinutes(10)),
                createCompletedSession(amrs.get(0), station1, now.minusHours(6), now.minusHours(5))
        ));

        WorkOrder workOrder = new WorkOrder();
        workOrder.setPlannedQty(200);
        workOrder.setPlannedStartDate(LocalDate.now().minusDays(1));
        workOrder.setPlannedEndDate(LocalDate.now().plusDays(7));
        workOrder.setStatus("RUNNING");
        workOrderRepository.save(workOrder);

        AmrTask completedTask = new AmrTask();
        completedTask.setAmr(amrs.get(0));
        completedTask.setTaskType("TRANSPORT");
        completedTask.setFromArea(loadArea);
        completedTask.setToArea(assemble01);
        completedTask.setStatus("COMPLETED");
        completedTask.setPickTime(now.minusMinutes(30));
        completedTask.setDropTime(now.minusMinutes(21));
        amrTaskRepository.save(completedTask);

        AmrTask activeTask = new AmrTask();
        activeTask.setAmr(amrs.get(1));
        activeTask.setTaskType("TRANSPORT");
        activeTask.setFromArea(assemble01);
        activeTask.setToArea(loadArea);
        activeTask.setStatus("IN_PROGRESS");
        activeTask.setPickTime(now.minusMinutes(8));
        amrTaskRepository.save(activeTask);

        AmrTask failedTask = new AmrTask();
        failedTask.setAmr(amrs.get(3));
        failedTask.setTaskType("TRANSPORT");
        failedTask.setFromArea(loadArea);
        failedTask.setToArea(assemble02);
        failedTask.setStatus("FAILED");
        failedTask.setPickTime(now.minusHours(2));
        failedTask.setDropTime(now.minusHours(1).minusMinutes(45));
        amrTaskRepository.save(failedTask);

        Alarm sampleAlarm = new Alarm();
        sampleAlarm.setSourceType("CHARGE_STATION");
        sampleAlarm.setSourceId("1");
        sampleAlarm.setLevel("warning");
        sampleAlarm.setMessage("충전 스테이션 1 혼잡 상태");
        sampleAlarm.setOccurredAt(now.minusDays(2));
        sampleAlarm.setAcknowledged(false);
        alarmRepository.save(sampleAlarm);
    }

    private Area createArea(
            Site site,
            String areaId,
            String areaName,
            String areaType,
            int tempMin,
            int tempMax,
            int humidityMin,
            int humidityMax,
            int particleMin,
            int particleMax,
            int coGasMin,
            int coGasMax
    ) {
        Area area = new Area();
        area.setAreaId(areaId);
        area.setSite(site);
        area.setAreaName(areaName);
        area.setAreaType(areaType);
        area.setTempMin(BigDecimal.valueOf(tempMin));
        area.setTempMax(BigDecimal.valueOf(tempMax));
        area.setHumidityMin(humidityMin);
        area.setHumidityMax(humidityMax);
        area.setParticleMin(particleMin);
        area.setParticleMax(particleMax);
        area.setCoGasMin(coGasMin);
        area.setCoGasMax(coGasMax);
        return area;
    }

    private Amr createAmr(int amrId, String amrName, double totalMileage) {
        Amr amr = new Amr();
        amr.setAmrId(amrId);
        amr.setAmrName(amrName);
        amr.setTotalMileage(totalMileage);
        amr.setLoadMax(600);
        amr.setBatteryCapacity(100);
        amr.setInspectionDt(LocalDate.now().minusDays(14));
        return amr;
    }

    private AmrStatusLog createStatusLog(
            Amr amr,
            Area area,
            String status,
            int posX,
            int posY,
            int yaw,
            int loadWeight,
            int batteryPct,
            int sohPct,
            float batteryTemp,
            LocalDateTime updatedAt
    ) {
        AmrStatusLog statusLog = new AmrStatusLog();
        statusLog.setAmr(amr);
        statusLog.setArea(area);
        statusLog.setStatus(status);
        statusLog.setPosX(posX);
        statusLog.setPosY(posY);
        statusLog.setYaw(yaw);
        statusLog.setLoadWeight(loadWeight);
        statusLog.setBatteryPct(batteryPct);
        statusLog.setSohPct(sohPct);
        statusLog.setBatteryTemp(batteryTemp);
        statusLog.setUpdatedAt(updatedAt);
        return statusLog;
    }

    private AmrChargeStation createStation(int stationId, Area area, String stationName, String stationStatus) {
        AmrChargeStation station = new AmrChargeStation();
        station.setStationId(stationId);
        station.setArea(area);
        station.setStationName(stationName);
        station.setStationStatus(stationStatus);
        return station;
    }

    private AmrChargingSession createActiveSession(
            Amr amr,
            AmrChargeStation station,
            String sessionStatus,
            LocalDateTime startTime
    ) {
        AmrChargingSession session = new AmrChargingSession();
        session.setAmr(amr);
        session.setStation(station);
        session.setSessionStatus(sessionStatus);
        session.setStartTime(startTime);
        return session;
    }

    private AmrChargingSession createCompletedSession(
            Amr amr,
            AmrChargeStation station,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        AmrChargingSession session = new AmrChargingSession();
        session.setAmr(amr);
        session.setStation(station);
        session.setSessionStatus("COMPLETED");
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        return session;
    }
}
