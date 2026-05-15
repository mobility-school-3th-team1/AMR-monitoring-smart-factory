package com.sfaas.amr_control_system.repository;

import com.sfaas.amr_control_system.entity.ProcessMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessMasterRepository extends JpaRepository<ProcessMaster, Integer> {
}