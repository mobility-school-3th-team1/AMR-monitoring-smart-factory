package com.sfaas.amr_control_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "AREA")
public class Area {

    @Id
    @Column(name = "area_id", length = 50)
    private String areaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "area_name", length = 100)
    private String areaName;

    @Column(name = "area_type", length = 50)
    private String areaType;

    @Column(name = "temp_min", precision = 10, scale = 1)
    private BigDecimal tempMin;

    @Column(name = "temp_max", precision = 10, scale = 1)
    private BigDecimal tempMax;

    @Column(name = "humidity_min")
    private Integer humidityMin;

    @Column(name = "humidity_max")
    private Integer humidityMax;

    @Column(name = "particle_min")
    private Integer particleMin;

    @Column(name = "particle_max")
    private Integer particleMax;

    @Column(name = "co_gas_min")
    private Integer coGasMin;

    @Column(name = "co_gas_max")
    private Integer coGasMax;
}
