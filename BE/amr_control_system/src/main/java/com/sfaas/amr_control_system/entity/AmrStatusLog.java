package com.sfaas.amr_control_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "AMR_STATUS_LOG")
public class AmrStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "amr_statlog_id")
    private Integer amrStatlogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amr_id")
    private Amr amr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private Area area;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "pos_x")
    private Integer posX;

    @Column(name = "pos_y")
    private Integer posY;

    @Column(name = "yaw")
    private Integer yaw;

    @Column(name = "load_weight")
    private Float loadWeight;

    @Column(name = "battery_pct")
    private Integer batteryPct;

    @Column(name = "soh_pct")
    private Integer sohPct;

    @Column(name = "battery_temp")
    private Float batteryTemp;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}