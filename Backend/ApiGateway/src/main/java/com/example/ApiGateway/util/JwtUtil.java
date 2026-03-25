package com.example.ApiGateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT utility for token validation and claim extraction
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:sangrah_secret_key_for_jwt_token_validation_please_change_in_production}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract user ID from token
     */
    public String extractUserId(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Extract role from token
     */
    public String extractRole(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("role");
    }

    /**
     * Extract email from token
     */
    public String extractEmail(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("email");
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * Get expiration date from token
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * Get all claims from token
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
