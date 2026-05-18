package com.sfaas.amr_control_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ENV_SENSOR_LOG")
public class EnvSensorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sensor_log_id")
    private Long sensorLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "env_sensor_id")
    private EnvSensor envSensor;

    @Column(name = "sensor_value", precision = 10, scale = 2)
    private BigDecimal sensorValue;

    @Column(name = "sensor_status", length = 50)
    private String sensorStatus;

    @Column(name = "measured_at")
    private LocalDateTime measuredAt;
}
