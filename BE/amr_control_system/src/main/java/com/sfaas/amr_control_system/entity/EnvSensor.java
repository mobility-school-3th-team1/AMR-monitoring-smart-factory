package com.sfaas.amr_control_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ENV_SENSOR")
public class EnvSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sensor_id")
    private Integer sensorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private Area area;

    @Column(name = "sensor_name", length = 100)
    private String sensorName;

    @Column(name = "sensor_type", length = 50)
    private String sensorType;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "min_val", precision = 10, scale = 2)
    private BigDecimal minVal;

    @Column(name = "max_val", precision = 10, scale = 2)
    private BigDecimal maxVal;

    @Column(name = "install_at")
    private LocalDateTime installAt;
}