package com.sfaas.amr_control_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "AMR_TASK")
public class AmrTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Integer taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amr_id")
    private Amr amr;

    @Column(name = "task_type", length = 50)
    private String taskType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private WipLot wipLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_area_id")
    private Area fromArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_area_id")
    private Area toArea;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "pick_time")
    private LocalDateTime pickTime;

    @Column(name = "drop_time")
    private LocalDateTime dropTime;
}