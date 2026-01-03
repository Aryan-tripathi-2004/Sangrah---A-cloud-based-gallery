package com.example.Auth.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.example.Auth.entity.Role;
import com.example.Auth.entity.User;
import com.example.Auth.common.constants.JwtConstants;
import com.example.Auth.common.exception.AuthenticationException;
import com.example.Auth.common.logging.DashLogger;
import com.example.Auth.common.logging.DashLoggerFactory;
import com.example.Auth.common.redis.RedisCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Provider for JWT token generation, validation, and parsing.
 * Uses HS512 (HMAC with SHA-512) for token signing.
 * Supports access tokens and refresh tokens with different expiration times.
 */
@Component
public class JwtTokenProvider {

    private static final DashLogger logger = DashLoggerFactory.getLogger(JwtTokenProvider.class);

    private final JWSSigner signer;
    private final JWSVerifier verifier;
    private final RedisCacheManager redisCacheManager;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-validity-seconds:900}") // 15 minutes default
    private long accessTokenValiditySeconds;

    @Value("${jwt.refresh-token-validity-seconds:2592000}") // 30 days default
    private long refreshTokenValiditySeconds;

    @Value("${jwt.issuer:sangrah-auth-service}")
    private String issuer;

    public JwtTokenProvider(RedisCacheManager redisCacheManager,
            @Value("${jwt.secret}") String jwtSecret) throws JOSEException {
        this.redisCacheManager = redisCacheManager;
        this.jwtSecret = jwtSecret;

        // Initialize signer and verifier with HS512
        byte[] secret = jwtSecret.getBytes();
        this.signer = new MACSigner(secret);
        this.verifier = new MACVerifier(secret);

        logger.info("JWT Token Provider initialized with HS512 signing algorithm");
    }

    /**
     * Generate an access token for a user.
     *
     * @param user      the user
     * @param sessionId the session ID
     * @return the JWT access token
     */
    public String generateAccessToken(User user, UUID sessionId) {
        try {
            Instant now = Instant.now();
            Instant expiryTime = now.plusSeconds(accessTokenValiditySeconds);

            List<String> roles = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(user.getId().toString())
                    .audience("sangrah-api")
                    .expirationTime(Date.from(expiryTime))
                    .notBeforeTime(Date.from(now))
                    .issueTime(Date.from(now))
                    .jwtID(UUID.randomUUID().toString())
                    .claim(JwtConstants.CLAIM_USER_ID, user.getId().toString())
                    .claim(JwtConstants.CLAIM_USERNAME, user.getUsername())
                    .claim(JwtConstants.CLAIM_EMAIL, user.getEmail())
                    .claim(JwtConstants.CLAIM_SESSION_ID, sessionId.toString())
                    .claim(JwtConstants.CLAIM_ROLES, roles)
                    .claim(JwtConstants.CLAIM_TOKEN_TYPE, JwtConstants.TOKEN_TYPE_ACCESS)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS512),
                    claimsSet);

            signedJWT.sign(signer);

            String token = signedJWT.serialize();

            logger.debug("Generated access token for user",
                    Map.of("userId", user.getId(),
                            "sessionId", sessionId,
                            "expiresIn", accessTokenValiditySeconds + "s"));

            return token;

        } catch (Exception e) {
            logger.error("Failed to generate access token", e, Map.of("userId", user.getId()));
            throw new AuthenticationException("ERR_TOKEN_GENERATION_FAILED",
                    "Failed to generate access token: " + e.getMessage());
        }
    }

    /**
     * Generate a refresh token for a user.
     *
     * @param user      the user
     * @param sessionId the session ID
     * @return the JWT refresh token
     */
    public String generateRefreshToken(User user, UUID sessionId) {
        try {
            Instant now = Instant.now();
            Instant expiryTime = now.plusSeconds(refreshTokenValiditySeconds);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(user.getId().toString())
                    .audience("sangrah-api")
                    .expirationTime(Date.from(expiryTime))
                    .notBeforeTime(Date.from(now))
                    .issueTime(Date.from(now))
                    .jwtID(UUID.randomUUID().toString())
                    .claim(JwtConstants.CLAIM_USER_ID, user.getId().toString())
                    .claim(JwtConstants.CLAIM_SESSION_ID, sessionId.toString())
                    .claim(JwtConstants.CLAIM_TOKEN_TYPE, JwtConstants.TOKEN_TYPE_REFRESH)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS512),
                    claimsSet);

            signedJWT.sign(signer);

            String token = signedJWT.serialize();

            logger.debug("Generated refresh token for user",
                    Map.of("userId", user.getId(),
                            "sessionId", sessionId,
                            "expiresIn", refreshTokenValiditySeconds + "s"));

            return token;

        } catch (Exception e) {
            logger.error("Failed to generate refresh token", e, Map.of("userId", user.getId()));
            throw new AuthenticationException("ERR_TOKEN_GENERATION_FAILED",
                    "Failed to generate refresh token: " + e.getMessage());
        }
    }

    /**
     * Validate a JWT token.
     * Checks signature, expiration, and blacklist status.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid
     */
    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Verify signature
            if (!signedJWT.verify(verifier)) {
                logger.warn("Token signature verification failed");
                return false;
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Check expiration
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                logger.debug("Token has expired");
                return false;
            }

            // Check if token is blacklisted
            String jti = claims.getJWTID();
            if (jti != null && isTokenBlacklisted(jti)) {
                logger.warn("Token is blacklisted", Map.of("jti", jti));
                return false;
            }

            // Check issuer
            if (!issuer.equals(claims.getIssuer())) {
                logger.warn("Invalid token issuer",
                        Map.of("expected", issuer, "actual", claims.getIssuer()));
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.warn("Token validation failed", Map.of("error", e.getMessage()));
            return false;
        }
    }

    /**
     * Parse and extract claims from a JWT token.
     *
     * @param token the JWT token
     * @return the JWT claims set
     * @throws AuthenticationException if parsing fails
     */
    public JWTClaimsSet parseToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!signedJWT.verify(verifier)) {
                throw new AuthenticationException("ERR_INVALID_TOKEN", "Token signature verification failed");
            }

            return signedJWT.getJWTClaimsSet();

        } catch (Exception e) {
            logger.warn("Failed to parse token", Map.of("error", e.getMessage()));
            throw new AuthenticationException("ERR_INVALID_TOKEN",
                    "Failed to parse token: " + e.getMessage());
        }
    }

    /**
     * Extract user ID from a JWT token.
     *
     * @param token the JWT token
     * @return the user ID
     */
    public UUID getUserIdFromToken(String token) {
        try {
            JWTClaimsSet claims = parseToken(token);
            String userIdStr = claims.getStringClaim(JwtConstants.CLAIM_USER_ID);
            return UUID.fromString(userIdStr);
        } catch (Exception e) {
            logger.warn("Failed to extract user ID from token", Map.of("error", e.getMessage()));
            throw new AuthenticationException("ERR_INVALID_TOKEN",
                    "Failed to extract user ID from token");
        }
    }

    /**
     * Extract session ID from a JWT token.
     *
     * @param token the JWT token
     * @return the session ID
     */
    public UUID getSessionIdFromToken(String token) {
        try {
            JWTClaimsSet claims = parseToken(token);
            String sessionIdStr = claims.getStringClaim(JwtConstants.CLAIM_SESSION_ID);
            return UUID.fromString(sessionIdStr);
        } catch (Exception e) {
            logger.warn("Failed to extract session ID from token", Map.of("error", e.getMessage()));
            throw new AuthenticationException("ERR_INVALID_TOKEN",
                    "Failed to extract session ID from token");
        }
    }

    /**
     * Extract username from a JWT token.
     *
     * @param token the JWT token
     * @return the username
     */
    public String getUsernameFromToken(String token) {
        try {
            JWTClaimsSet claims = parseToken(token);
            return claims.getStringClaim(JwtConstants.CLAIM_USERNAME);
        } catch (Exception e) {
            logger.warn("Failed to extract username from token", Map.of("error", e.getMessage()));
            return null;
        }
    }

    /**
     * Extract roles from a JWT token.
     *
     * @param token the JWT token
     * @return list of role names
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            JWTClaimsSet claims = parseToken(token);
            return (List<String>) claims.getClaim(JwtConstants.CLAIM_ROLES);
        } catch (Exception e) {
            logger.warn("Failed to extract roles from token", Map.of("error", e.getMessage()));
            return List.of();
        }
    }

    /**
     * Get the token type (ACCESS or REFRESH).
     *
     * @param token the JWT token
     * @return the token type
     */
    public String getTokenType(String token) {
        try {
            JWTClaimsSet claims = parseToken(token);
            return claims.getStringClaim(JwtConstants.CLAIM_TOKEN_TYPE);
        } catch (Exception e) {
            logger.warn("Failed to extract token type from token", Map.of("error", e.getMessage()));
            return null;
        }
    }

    /**
     * Get the remaining validity time of a token in seconds.
     *
     * @param token the JWT token
     * @return seconds until expiration, or 0 if expired
     */
    public long getRemainingValiditySeconds(String token) {
        try {
            JWTClaimsSet claims = parseToken(token);
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null) {
                return 0;
            }
            long remainingMs = expirationTime.getTime() - System.currentTimeMillis();
            return Math.max(0, remainingMs / 1000);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Blacklist a token (add to Redis with TTL).
     * Used for logout functionality.
     *
     * @param token the JWT token to blacklist
     */
    public void blacklistToken(String token) {
        try {
            JWTClaimsSet claims = parseToken(token);
            String jti = claims.getJWTID();

            if (jti != null) {
                long remainingSeconds = getRemainingValiditySeconds(token);
                if (remainingSeconds > 0) {
                    String blacklistKey = "blacklist:token:" + jti;
                    redisCacheManager.set(blacklistKey, "true", remainingSeconds);
                    logger.info("Token blacklisted",
                            Map.of("jti", jti, "ttl", remainingSeconds + "s"));
                }
            }
        } catch (Exception e) {
            logger.error("Failed to blacklist token", e);
        }
    }

    /**
     * Check if a token is blacklisted.
     *
     * @param jti the JWT ID (jti claim)
     * @return true if the token is blacklisted
     */
    public boolean isTokenBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        String blacklistKey = "blacklist:token:" + jti;
        return redisCacheManager.exists(blacklistKey);
    }

    /**
     * Get access token validity in seconds.
     *
     * @return validity in seconds
     */
    public long getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    /**
     * Get refresh token validity in seconds.
     *
     * @return validity in seconds
     */
    public long getRefreshTokenValiditySeconds() {
        return refreshTokenValiditySeconds;
    }
}
