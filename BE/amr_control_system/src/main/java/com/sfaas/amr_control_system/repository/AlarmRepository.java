package com.sfaas.amr_control_system.repository;

import com.sfaas.amr_control_system.entity.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    long countByAcknowledgedFalse();

    List<Alarm> findByAcknowledgedFalseOrderByOccurredAtDesc();

    List<Alarm> findAllByOrderByOccurredAtDesc();
}
