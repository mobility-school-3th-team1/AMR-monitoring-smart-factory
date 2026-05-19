package com.sfaas.amr_control_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "AMR_COMMAND")
public class AmrCommand {

    @Id
    @Column(name = "command_id", length = 50)
    private String commandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amr_id")
    private Amr amr;

    @Column(name = "command_type", length = 50)
    private String commandType;

    @Column(name = "params", columnDefinition = "TEXT")
    private String params;

    @Column(name = "accepted")
    private Boolean accepted;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;
}
