package com.sfaas.amr_control_system.config;

import com.sfaas.amr_control_system.entity.Amr;
import com.sfaas.amr_control_system.entity.AmrChargeStation;
import com.sfaas.amr_control_system.entity.AmrChargingSession;
import com.sfaas.amr_control_system.entity.AmrStatusLog;
import com.sfaas.amr_control_system.entity.AmrTask;
import com.sfaas.amr_control_system.entity.Area;
import com.sfaas.amr_control_system.entity.Site;
import com.sfaas.amr_control_system.entity.WorkOrder;
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

    @Override
    public void run(String... args) {
        if (amrRepository.count() > 0) {
            return;
        }

        log.info("Seeding dashboard demo data (H2).");

        Site site = new Site();
        site.setSiteId("site-001");
        site.setSiteName("스마트팩토리 1공장");
        siteRepository.save(site);

        Area warehouse = createArea(site, "원자재 창고", "warehouse");
        Area assembly = createArea(site, "조립 구역", "assembly");
        areaRepository.saveAll(List.of(warehouse, assembly));

        List<Amr> amrs = List.of(
                createAmr("AMR-01"),
                createAmr("AMR-02"),
                createAmr("AMR-03"),
                createAmr("AMR-04")
        );
        amrRepository.saveAll(amrs);

        LocalDateTime now = LocalDateTime.now();
        amrStatusLogRepository.saveAll(List.of(
                createStatusLog(amrs.get(0), warehouse, "operating", 90, now.minusMinutes(30), 50, 100),
                createStatusLog(amrs.get(0), warehouse, "operating", 88, now.minusMinutes(20), 80, 120),
                createStatusLog(amrs.get(0), assembly, "operating", 86, now.minusMinutes(2), 123, 57),
                createStatusLog(amrs.get(1), assembly, "charging", 72, now.minusMinutes(5), 140, 70),
                createStatusLog(amrs.get(2), warehouse, "waiting", 45, now.minusMinutes(1), 100, 200),
                createStatusLog(amrs.get(3), assembly, "error", 20, now.minusMinutes(3), 130, 65)
        ));

        AmrChargeStation congestedStation = new AmrChargeStation();
        congestedStation.setArea(warehouse);
        congestedStation.setStationName("충전 스테이션 3");
        congestedStation.setStationStatus("혼잡");

        AmrChargeStation normalStation = new AmrChargeStation();
        normalStation.setArea(assembly);
        normalStation.setStationName("충전 스테이션 1");
        normalStation.setStationStatus("normal");

        amrChargeStationRepository.saveAll(List.of(congestedStation, normalStation));

        amrChargingSessionRepository.saveAll(List.of(
                createActiveSession(amrs.get(1), congestedStation, "charging", now.minusMinutes(25)),
                createActiveSession(amrs.get(2), congestedStation, "waiting", now.minusMinutes(10)),
                createCompletedSession(amrs.get(0), normalStation, now.minusHours(6), now.minusHours(5))
        ));

        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo("WO-2026-001");
        workOrder.setPlannedQty(156);
        workOrder.setPlannedStartDate(LocalDate.now().minusDays(1));
        workOrder.setPlannedEndDate(LocalDate.now().plusDays(7));
        workOrder.setStatus("in_progress");
        workOrderRepository.save(workOrder);

        AmrTask completedTask = new AmrTask();
        completedTask.setAmr(amrs.get(0));
        completedTask.setTaskType("transport");
        completedTask.setFromArea(warehouse);
        completedTask.setToArea(assembly);
        completedTask.setStatus("completed");
        completedTask.setPickTime(now.minusMinutes(30));
        completedTask.setDropTime(now.minusMinutes(21));
        amrTaskRepository.save(completedTask);

        AmrTask activeTask = new AmrTask();
        activeTask.setAmr(amrs.get(1));
        activeTask.setTaskType("transport");
        activeTask.setFromArea(assembly);
        activeTask.setToArea(warehouse);
        activeTask.setStatus("in_progress");
        activeTask.setPickTime(now.minusMinutes(8));
        amrTaskRepository.save(activeTask);
    }

    private Area createArea(Site site, String areaName, String areaType) {
        Area area = new Area();
        area.setSite(site);
        area.setAreaName(areaName);
        area.setAreaType(areaType);
        area.setTempMin(BigDecimal.valueOf(18));
        area.setTempMax(BigDecimal.valueOf(28));
        area.setHumidityMin(BigDecimal.valueOf(30));
        area.setHumidityMax(BigDecimal.valueOf(60));
        return area;
    }

    private Amr createAmr(String amrName) {
        Amr amr = new Amr();
        amr.setAmrName(amrName);
        amr.setTotalMileage(1200.0);
        amr.setLoadMax(500);
        amr.setBatteryCapacity(100);
        amr.setInspectionDt(LocalDateTime.now().minusDays(14));
        return amr;
    }

    private AmrStatusLog createStatusLog(
            Amr amr,
            Area area,
            String status,
            int batteryPct,
            LocalDateTime updatedAt,
            int posX,
            int posY
    ) {
        AmrStatusLog statusLog = new AmrStatusLog();
        statusLog.setAmr(amr);
        statusLog.setArea(area);
        statusLog.setStatus(status);
        statusLog.setPosX(posX);
        statusLog.setPosY(posY);
        statusLog.setYaw(90);
        statusLog.setLoadWeight(120.5f);
        statusLog.setBatteryPct(batteryPct);
        statusLog.setSohPct(95);
        statusLog.setBatteryTemp(32.5f);
        statusLog.setUpdatedAt(updatedAt);
        return statusLog;
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
        session.setSessionStatus("completed");
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        return session;
    }
}
