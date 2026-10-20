package com.example.ApiGateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;

/**
 * JWT utility for token validation and claim extraction
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.claim.role-key}")
    private String roleClaimKey = "role";

    @Value("${jwt.claim.email-key}")
    private String emailClaimKey = "email";

    @Value("${jwt.claim.type-key}")
    private String typeClaimKey = "type";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parse and cryptographically verify the JWT token once, returning all claims.
     */
    public Claims extractAllClaims(String token) {
        return getAllClaimsFromToken(token);
    }

    /**
     * Validate JWT token
     */
    public void validateToken(String token) {
        extractAllClaims(token);
    }

    /**
     * Extract user ID from token
     */
    public String extractUserId(String token) {
        return extractUserId(extractAllClaims(token));
    }

    /**
     * Extract user ID from pre-parsed claims
     */
    public String extractUserId(Claims claims) {
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * Extract role from token
     */
    public String extractRole(String token) {
        return extractRole(extractAllClaims(token));
    }

    /**
     * Extract role from pre-parsed claims
     */
    public String extractRole(Claims claims) {
        return claims != null ? (String) claims.get(roleClaimKey) : null;
    }

    /**
     * Extract email from token
     */
    public String extractEmail(String token) {
        return extractEmail(extractAllClaims(token));
    }

    /**
     * Extract email from pre-parsed claims
     */
    public String extractEmail(Claims claims) {
        return claims != null ? (String) claims.get(emailClaimKey) : null;
    }

    /**
     * Extract token type from token
     */
    public String extractTokenType(String token) {
        return extractTokenType(extractAllClaims(token));
    }

    /**
     * Extract token type from pre-parsed claims
     */
    public String extractTokenType(Claims claims) {
        return claims != null ? (String) claims.get(typeClaimKey) : null;
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration != null && expiration.before(new Date());
    }

    /**
     * Get expiration date from token
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? claims.getExpiration() : null;
    }

    /**
     * Get all claims from token
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getRoleClaimKey() {
        return roleClaimKey;
    }

    public String getEmailClaimKey() {
        return emailClaimKey;
    }

    public String getTypeClaimKey() {
        return typeClaimKey;
    }
}
