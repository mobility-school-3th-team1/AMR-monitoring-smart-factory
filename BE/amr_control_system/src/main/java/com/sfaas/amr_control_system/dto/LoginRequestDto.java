package com.sfaas.amr_control_system.dto;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String username;
    private String password;
}