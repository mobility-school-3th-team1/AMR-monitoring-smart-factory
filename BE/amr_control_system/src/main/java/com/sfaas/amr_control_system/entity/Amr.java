package com.sfaas.amr_control_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "AMR")
public class Amr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "amr_id")
    private Integer amrId;

    @Column(name = "amr_name", length = 100)
    private String amrName;

    @Column(name = "total_mileage")
    private Double totalMileage;

    @Column(name = "load_max")
    private Integer loadMax;

    @Column(name = "battery_capacity")
    private Integer batteryCapacity;

    @Column(name = "inspection_dt")
    private LocalDateTime inspectionDt;
}