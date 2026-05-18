package com.sfaas.amr_control_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "PR_ROUTING")
public class Routing {

    @Id
    @Column(name = "pr_routing_id", length = 50)
    private String prRoutingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pr_process_id")
    private ProcessMaster processMaster;

    @Column(name = "seq_no")
    private Integer seqNo;

    @Column(name = "standard_lead_time")
    private Integer standardLeadTime;
}
