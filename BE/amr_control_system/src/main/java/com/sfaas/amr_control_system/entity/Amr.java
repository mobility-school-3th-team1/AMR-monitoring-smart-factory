package com.sfaas.amr_control_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "AMR_MASTER")
public class Amr {

    @Id
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
    private LocalDate inspectionDt;
}
