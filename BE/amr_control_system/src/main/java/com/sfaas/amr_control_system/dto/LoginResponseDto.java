package com.sfaas.amr_control_system.dto;

import lombok.Data;

@Data
public class LoginResponseDto {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Integer expiresIn;
    private UserDto user;
}