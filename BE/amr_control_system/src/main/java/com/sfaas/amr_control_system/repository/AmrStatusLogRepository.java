package com.sfaas.amr_control_system.repository;

import com.sfaas.amr_control_system.entity.AmrStatusLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AmrStatusLogRepository extends JpaRepository<AmrStatusLog, Integer> {

    List<AmrStatusLog> findAllByOrderByUpdatedAtDesc();

    Page<AmrStatusLog> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    Optional<AmrStatusLog> findFirstByAmr_AmrIdOrderByUpdatedAtDesc(Integer amrId);

    List<AmrStatusLog> findByAmr_AmrIdOrderByUpdatedAtDesc(Integer amrId);

    List<AmrStatusLog> findByAmr_AmrIdAndUpdatedAtBetweenOrderByUpdatedAtAsc(
            Integer amrId,
            LocalDateTime from,
            LocalDateTime to
    );
}