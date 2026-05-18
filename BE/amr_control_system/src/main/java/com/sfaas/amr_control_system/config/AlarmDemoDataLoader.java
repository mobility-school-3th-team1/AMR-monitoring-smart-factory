package com.sfaas.amr_control_system.config;

import com.sfaas.amr_control_system.entity.Alarm;
import com.sfaas.amr_control_system.entity.AmrChargeStation;
import com.sfaas.amr_control_system.entity.AmrStatusLog;
import com.sfaas.amr_control_system.repository.AlarmRepository;
import com.sfaas.amr_control_system.repository.AmrChargeStationRepository;
import com.sfaas.amr_control_system.repository.AmrStatusLogRepository;
import com.sfaas.amr_control_system.util.DashboardStatusNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class AlarmDemoDataLoader implements CommandLineRunner {

    private final AlarmRepository alarmRepository;
    private final AmrStatusLogRepository amrStatusLogRepository;
    private final AmrChargeStationRepository amrChargeStationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (alarmRepository.count() > 0) {
            return;
        }
        if (amrStatusLogRepository.count() == 0) {
            return;
        }

        log.info("Seeding alarm demo data from operational records (H2).");

        List<Alarm> alarms = new ArrayList<>();

        for (AmrStatusLog statusLog : amrStatusLogRepository.findAllByOrderByUpdatedAtDesc()) {
            if (!"error".equals(DashboardStatusNormalizer.normalizeAmrStatus(statusLog.getStatus()))) {
                continue;
            }
            Alarm alarm = new Alarm();
            alarm.setSourceType("amr");
            alarm.setSourceId(statusLog.getAmr() != null
                    ? String.format("amr-%02d", statusLog.getAmr().getAmrId())
                    : null);
            alarm.setLevel("critical");
            alarm.setMessage(String.format(
                    "AMR %s 오류 상태 감지",
                    statusLog.getAmr() != null ? statusLog.getAmr().getAmrName() : "unknown"
            ));
            alarm.setOccurredAt(statusLog.getUpdatedAt());
            alarm.setAcknowledged(false);
            alarms.add(alarm);
        }

        for (AmrChargeStation station : amrChargeStationRepository.findAll()) {
            if (!DashboardStatusNormalizer.isCongestedStation(station.getStationStatus())) {
                continue;
            }
            Alarm alarm = new Alarm();
            alarm.setSourceType("station");
            alarm.setSourceId("station-" + station.getStationId());
            alarm.setLevel("warning");
            alarm.setMessage(String.format("%s 혼잡 상태", station.getStationName()));
            alarm.setOccurredAt(LocalDateTime.now());
            alarm.setAcknowledged(false);
            alarms.add(alarm);
        }

        if (!alarms.isEmpty()) {
            alarmRepository.saveAll(alarms);
        }
    }
}
