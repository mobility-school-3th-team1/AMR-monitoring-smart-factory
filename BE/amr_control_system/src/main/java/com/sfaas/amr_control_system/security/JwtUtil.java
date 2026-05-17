package com.sfaas.amr_control_system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long accessTokenExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenExpirationMs;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractJti(String token) {
        return extractClaim(token, claims -> claims.get(JwtConstants.CLAIM_JTI, String.class));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(JwtConstants.CLAIM_TOKEN_TYPE, String.class));
    }

    public int getAccessTokenExpiresInSeconds() {
        return (int) (accessTokenExpirationMs / 1000);
    }

    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        Claims claims = parseClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(
                userDetails.getUsername(),
                JwtConstants.TOKEN_TYPE_ACCESS,
                accessTokenExpirationMs,
                null
        );
    }

    public String generateRefreshToken(String username) {
        String jti = UUID.randomUUID().toString();
        return buildToken(
                username,
                JwtConstants.TOKEN_TYPE_REFRESH,
                refreshTokenExpirationMs,
                jti
        );
    }

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        if (!JwtConstants.TOKEN_TYPE_ACCESS.equals(extractTokenType(token))) {
            return false;
        }
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isRefreshTokenValid(String token) {
        if (!JwtConstants.TOKEN_TYPE_REFRESH.equals(extractTokenType(token))) {
            return false;
        }
        return !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private String buildToken(String subject, String tokenType, long expirationMs, String jti) {
        var builder = Jwts.builder()
                .subject(subject)
                .claim(JwtConstants.CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSignInKey());

        if (jti != null) {
            builder.claim(JwtConstants.CLAIM_JTI, jti);
        }

        return builder.compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
