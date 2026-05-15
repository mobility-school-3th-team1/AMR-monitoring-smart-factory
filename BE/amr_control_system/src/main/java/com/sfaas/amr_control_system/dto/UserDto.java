package com.sfaas.amr_control_system.dto;

import lombok.Data;

@Data
public class UserDto {
    private String id;
    private String username;
    private String displayName;
    private String role;
}