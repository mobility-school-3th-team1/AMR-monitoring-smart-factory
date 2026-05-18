package com.sfaas.amr_control_system.service;

import com.sfaas.amr_control_system.dto.LoginRequestDto;
import com.sfaas.amr_control_system.dto.LoginResponseDto;
import com.sfaas.amr_control_system.dto.RefreshTokenRequestDto;
import com.sfaas.amr_control_system.dto.RefreshTokenResponseDto;
import com.sfaas.amr_control_system.dto.UserDto;
import com.sfaas.amr_control_system.entity.User;
import com.sfaas.amr_control_system.exception.InvalidRefreshTokenException;
import com.sfaas.amr_control_system.repository.UserRepository;
import com.sfaas.amr_control_system.security.JwtConstants;
import com.sfaas.amr_control_system.security.JwtUtil;
import com.sfaas.amr_control_system.security.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenStore refreshTokenStore;

    public LoginResponseDto login(LoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = issueRefreshToken(userDetails.getUsername());

        LoginResponseDto response = new LoginResponseDto();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType(JwtConstants.TOKEN_TYPE_BEARER);
        response.setExpiresIn(jwtUtil.getAccessTokenExpiresInSeconds());
        response.setUser(toUserDto(user));
        return response;
    }

    public RefreshTokenResponseDto refresh(RefreshTokenRequestDto request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("refresh token이 필요합니다.");
        }

        if (!jwtUtil.isRefreshTokenValid(refreshToken)) {
            throw new InvalidRefreshTokenException("유효하지 않은 refresh token입니다.");
        }

        String jti = jwtUtil.extractJti(refreshToken);
        if (!refreshTokenStore.isRegistered(jti)) {
            throw new InvalidRefreshTokenException("만료되었거나 무효화된 refresh token입니다.");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String accessToken = jwtUtil.generateAccessToken(userDetails);

        RefreshTokenResponseDto response = new RefreshTokenResponseDto();
        response.setAccessToken(accessToken);
        response.setTokenType(JwtConstants.TOKEN_TYPE_BEARER);
        response.setExpiresIn(jwtUtil.getAccessTokenExpiresInSeconds());
        return response;
    }

    public void logout(String username) {
        refreshTokenStore.revokeAllForUser(username);
    }

    private String issueRefreshToken(String username) {
        String refreshToken = jwtUtil.generateRefreshToken(username);
        String jti = jwtUtil.extractJti(refreshToken);
        refreshTokenStore.register(jti, username);
        return refreshToken;
    }

    private UserDto toUserDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getUserId());
        userDto.setUsername(user.getUsername());
        userDto.setDisplayName(user.getDisplayName());
        userDto.setRole(user.getRole());
        return userDto;
    }
}
