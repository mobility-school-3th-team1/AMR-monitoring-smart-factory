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
import java.time.LocalDate;

@Data
@Entity
@Table(name = "ENV_SENSOR")
public class EnvSensor {

    @Id
    @Column(name = "env_sensor_id", length = 50)
    private String envSensorId;

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
    private LocalDate installAt;
}
