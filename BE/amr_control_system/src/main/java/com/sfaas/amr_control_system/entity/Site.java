package com.sfaas.amr_control_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "SITE")
public class Site {

    @Id
    @Column(name = "site_id", length = 50)
    private String siteId;

    @Column(name = "site_name", length = 100)
    private String siteName;
}