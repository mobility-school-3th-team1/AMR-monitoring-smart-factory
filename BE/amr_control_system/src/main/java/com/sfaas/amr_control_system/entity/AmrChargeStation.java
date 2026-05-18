package com.sfaas.amr_control_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "AMR_CHARGE_STATION")
public class AmrChargeStation {

    @Id
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
