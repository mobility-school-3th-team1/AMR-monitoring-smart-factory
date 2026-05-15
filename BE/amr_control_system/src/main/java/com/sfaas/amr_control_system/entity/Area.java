package com.sfaas.amr_control_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "AREA")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "area_id")
    private Integer areaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "area_name", length = 100)
    private String areaName;

    @Column(name = "area_type", length = 50)
    private String areaType;

    @Column(name = "temp_min", precision = 5, scale = 2)
    private BigDecimal tempMin;

    @Column(name = "temp_max", precision = 5, scale = 2)
    private BigDecimal tempMax;

    @Column(name = "humidity_min", precision = 5, scale = 2)
    private BigDecimal humidityMin;

    @Column(name = "humidity_max", precision = 5, scale = 2)
    private BigDecimal humidityMax;
}