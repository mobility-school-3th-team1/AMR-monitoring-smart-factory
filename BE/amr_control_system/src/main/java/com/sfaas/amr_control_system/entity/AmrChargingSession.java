package com.sfaas.amr_control_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "AMR_CHARGING_SESSION")
public class AmrChargingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "charging_session_id")
    private Long chargingSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amr_id")
    private Amr amr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private AmrChargeStation station;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "session_status", length = 50)
    private String sessionStatus;
}