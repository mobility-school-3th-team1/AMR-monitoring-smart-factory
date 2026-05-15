package com.sfaas.amr_control_system.dto;

import lombok.Data;

@Data
public class RefreshTokenResponseDto {
    private String accessToken;
    private String tokenType;
    private Integer expiresIn;
}