package com.sfaas.amr_control_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "AMR_CHARGE_STATION")
public class AmrChargeStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "station_id")
    private Integer stationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private Area area;

    @Column(name = "station_name", length = 100)
    private String stationName;

    @Column(name = "station_status", length = 50)
    private String stationStatus;
}