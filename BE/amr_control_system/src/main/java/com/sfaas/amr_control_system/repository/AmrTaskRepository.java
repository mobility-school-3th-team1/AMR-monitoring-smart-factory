package com.sfaas.amr_control_system.repository;

import com.sfaas.amr_control_system.entity.AmrTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmrTaskRepository extends JpaRepository<AmrTask, Long>, JpaSpecificationExecutor<AmrTask> {

    List<AmrTask> findByPickTimeIsNotNullAndDropTimeIsNotNull();

    Optional<AmrTask> findFirstByAmr_AmrIdAndDropTimeIsNullOrderByPickTimeDesc(Integer amrId);

    List<AmrTask> findByStatusAndDropTimeIsNull(String status);
}
