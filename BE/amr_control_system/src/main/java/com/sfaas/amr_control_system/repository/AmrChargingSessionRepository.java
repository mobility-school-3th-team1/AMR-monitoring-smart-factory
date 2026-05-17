package com.sfaas.amr_control_system.repository;

import com.sfaas.amr_control_system.entity.AmrChargingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AmrChargingSessionRepository extends JpaRepository<AmrChargingSession, Long> {

    List<AmrChargingSession> findByEndTimeIsNull();

    List<AmrChargingSession> findByStation_StationId(Integer stationId);

    List<AmrChargingSession> findByEndTimeIsNotNull();

    List<AmrChargingSession> findByStartTimeBetween(LocalDateTime from, LocalDateTime to);

    List<AmrChargingSession> findByAmr_AmrIdAndStartTimeBetween(Integer amrId, LocalDateTime from, LocalDateTime to);

    List<AmrChargingSession> findByStation_StationIdAndStartTimeBetween(
            Integer stationId,
            LocalDateTime from,
            LocalDateTime to
    );
}
