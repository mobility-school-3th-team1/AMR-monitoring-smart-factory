package com.sfaas.amr_control_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ENV_READING")
public class EnvReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reading_id")
    private Long readingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id")
    private EnvSensor sensor;

    @Column(name = "measured_at")
    private LocalDateTime measuredAt;

    @Column(name = "value_num", precision = 10, scale = 2)
    private BigDecimal valueNum;

    @Column(name = "status", length = 50)
    private String status;
}