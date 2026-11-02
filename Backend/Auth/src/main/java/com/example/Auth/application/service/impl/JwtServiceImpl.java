package com.example.Auth.application.service.impl;

import com.example.Auth.application.service.interfaces.IJwtService;
import com.example.Auth.shared.enums.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtServiceImpl implements IJwtService {
    private final SecretKey secretKey;
    private final long accessTokenExpirySeconds;
    private final long refreshTokenExpirySeconds;
    private final String defaultRole;

    public JwtServiceImpl(@Value("${jwt.secret}") String secret,
                          @Value("${jwt.access-token-expiration-seconds}") long accessTokenExpirySeconds,
                          @Value("${jwt.refresh-token-expiration-seconds}") long refreshTokenExpirySeconds,
                          @Value("${jwt.default-role}") String defaultRole) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirySeconds = accessTokenExpirySeconds;
        this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
        this.defaultRole = defaultRole;
    }

    @Override
    public String generateAccessToken(String userId, String email) {
        return generateAccessToken(userId, email, Set.of());
    }

    @Override
    public String generateAccessToken(String userId, String email, Set<Role> roles) {
        Instant now = Instant.now();
        String resolvedRole = (roles != null && roles.contains(Role.ADMIN))
                ? Role.ADMIN.name()
                : defaultRole;

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("role", resolvedRole)
                .claim("type", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenExpirySeconds)))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("type", "REFRESH")
                .claim("jti", UUID.randomUUID().toString())  // Unique token ID for blacklist checks
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenExpirySeconds)))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public long getRefreshTokenExpirySeconds() {
        return refreshTokenExpirySeconds;
    }

    @Override
    public String extractUserId(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            return e.getClaims() != null ? e.getClaims().getSubject() : null;
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
