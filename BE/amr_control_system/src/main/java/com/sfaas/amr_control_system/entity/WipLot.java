package com.sfaas.amr_control_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "WIP_LOT")
public class WipLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lot_id")
    private Integer lotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wo_id")
    private WorkOrder workOrder;

    @Column(name = "lot_no", length = 50)
    private String lotNo;

    @Column(name = "current_qty")
    private Integer currentQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_routing_id")
    private Routing currentRouting;

    @Column(name = "status", length = 50)
    private String status;
}