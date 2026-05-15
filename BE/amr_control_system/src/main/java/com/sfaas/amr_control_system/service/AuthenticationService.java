package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.dto.LoginRequestDto;
import com.sfaas.amr_control_system.dto.LoginResponseDto;
import com.sfaas.amr_control_system.dto.UserDto;
import com.sfaas.amr_control_system.entity.User;
import com.sfaas.amr_control_system.repository.UserRepository;
import com.sfaas.amr_control_system.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public LoginResponseDto login(LoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtUtil.generateToken(userDetails);

        UserDto userDto = new UserDto();
        userDto.setId(user.getId().toString());
        userDto.setUsername(user.getUsername());
        userDto.setDisplayName(user.getDisplayName());
        userDto.setRole(user.getRole());

        LoginResponseDto response = new LoginResponseDto();
        response.setAccessToken(accessToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(3600); // 1 hour
        response.setUser(userDto);

        return response;
    }
}