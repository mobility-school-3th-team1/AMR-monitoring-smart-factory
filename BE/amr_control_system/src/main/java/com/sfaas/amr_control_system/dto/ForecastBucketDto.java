package com.sfaas.amr_control_system.dto;

import lombok.Data;

@Data
public class ForecastBucketDto {
    private String bucket;
    private Integer count;
}