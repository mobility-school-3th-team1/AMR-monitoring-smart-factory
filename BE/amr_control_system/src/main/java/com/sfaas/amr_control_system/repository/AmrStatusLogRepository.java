package com.sfaas.amr_control_system.repository;

import com.sfaas.amr_control_system.entity.AmrStatusLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmrStatusLogRepository extends JpaRepository<AmrStatusLog, Integer> {

    List<AmrStatusLog> findAllByOrderByUpdatedAtDesc();

    Page<AmrStatusLog> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}