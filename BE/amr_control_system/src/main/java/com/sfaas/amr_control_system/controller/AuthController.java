package com.sfaas.amr_control_system.controller;

import com.sfaas.amr_control_system.dto.LoginRequestDto;
import com.sfaas.amr_control_system.dto.LoginResponseDto;
import com.sfaas.amr_control_system.dto.RefreshTokenRequestDto;
import com.sfaas.amr_control_system.dto.RefreshTokenResponseDto;
import com.sfaas.amr_control_system.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request) {
        LoginResponseDto response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDto> refresh(@RequestBody RefreshTokenRequestDto request) {
        RefreshTokenResponseDto response = authenticationService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authenticationService.logout(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
