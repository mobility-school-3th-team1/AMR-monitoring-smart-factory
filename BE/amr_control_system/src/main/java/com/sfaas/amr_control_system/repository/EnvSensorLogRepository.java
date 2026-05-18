package com.sfaas.amr_control_system.repository;

import com.sfaas.amr_control_system.entity.EnvSensorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnvSensorLogRepository extends JpaRepository<EnvSensorLog, Long> {
}
