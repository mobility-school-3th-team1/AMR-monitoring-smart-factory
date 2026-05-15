package com.sfaas.amr_control_system.repository;

import com.sfaas.amr_control_system.entity.AmrChargeStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AmrChargeStationRepository extends JpaRepository<AmrChargeStation, Integer> {
}