package com.example.Auth.service;

import com.example.Auth.dao.RefreshTokenDao;
import com.example.Auth.dao.SessionDao;
import com.example.Auth.dto.LoginRequest;
import com.example.Auth.dto.LoginResponse;
import com.example.Auth.dto.RefreshTokenRequest;
import com.example.Auth.dto.RefreshTokenResponse;
import com.example.Auth.entity.RefreshToken;
import com.example.Auth.entity.Session;
import com.example.Auth.entity.User;
import com.example.Auth.entity.UserProfile;
import com.example.Auth.repository.RefreshTokenRepository;
import com.example.Auth.repository.SessionRepository;
import com.example.Auth.repository.UserProfileRepository;
import com.example.Auth.repository.UserRepository;
import com.example.Auth.security.JwtTokenProvider;
import com.example.Auth.common.exception.AuthenticationException;
import com.example.Auth.common.logging.DashLogger;
import com.example.Auth.common.logging.DashLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.example.Auth.util.MessageConstants.*;

/**
 * Service for authentication operations including login, token refresh, and
 * logout.
 */
@Service
public class AuthenticationService {

    private static final DashLogger logger = DashLoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenDao refreshTokenDao;
    private final SessionDao sessionDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.access-token-expiry-seconds:900}")
    private long accessTokenExpiry;

    @Value("${app.jwt.refresh-token-expiry-seconds:2592000}")
    private long refreshTokenExpiry;

    @Value("${app.security.max-failed-login-attempts:5}")
    private int maxFailedLoginAttempts;

    @Value("${app.security.account-lockout-duration-minutes:30}")
    private int accountLockoutDurationMinutes;

    public AuthenticationService(UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenDao refreshTokenDao,
            SessionDao sessionDao,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenDao = refreshTokenDao;
        this.sessionDao = sessionDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Authenticates a user and generates JWT tokens.
     *
     * @param request     The login request
     * @param httpRequest The HTTP request for extracting IP and user agent
     * @return LoginResponse with access and refresh tokens
     * @throws AuthenticationException if authentication fails
     */
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        logger.info(MSG_LOGIN_ATTEMPT, Map.of(
                "usernameOrEmail", request.getUsernameOrEmail()));

        // Find user by username or email
        User user = userRepository.findByUsernameOrEmail(
                request.getUsernameOrEmail(),
                request.getUsernameOrEmail()).orElseThrow(() -> {
                    logger.warn(MSG_LOGIN_FAILED_USER_NOT_FOUND, Map.of(
                            "usernameOrEmail", request.getUsernameOrEmail()));
                    return new AuthenticationException("Invalid username/email or password");
                });

        // Check if account is locked
        if (Boolean.TRUE.equals(user.getLocked())) {
            logger.warn(MSG_LOGIN_FAILED_ACCOUNT_LOCKED, Map.of(
                    "userId", user.getId().toString(),
                    "username", user.getUsername()));
            throw new AuthenticationException("Account is locked. Please contact support.");
        }

        // Check if account is enabled
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            logger.warn(MSG_LOGIN_FAILED_ACCOUNT_DISABLED, Map.of(
                    "userId", user.getId().toString(),
                    "username", user.getUsername()));
            throw new AuthenticationException("Account is disabled");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            logger.warn(MSG_LOGIN_FAILED_INVALID_PASSWORD, Map.of(
                    "userId", user.getId().toString(),
                    "username", user.getUsername()));
            throw new AuthenticationException("Invalid username/email or password");
        }

        // Reset failed login attempts on successful login
        resetFailedLoginAttempts(user);

        // Get user profile
        UserProfile userProfile = userProfileRepository.findByUserId(user.getId())
                .orElse(null);

        String displayName = userProfile != null ? userProfile.getDisplayName() : user.getUsername();

        // Extract request metadata
        String ipAddress = extractIpAddress(httpRequest);
        String userAgent = request.getUserAgent() != null
                ? request.getUserAgent()
                : httpRequest.getHeader("User-Agent");

        // Create session
        Session session = createSession(user, ipAddress, userAgent);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user, session.getSessionId());

        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user, session.getSessionId());

        // Save refresh token
        long expirySeconds = request.getRememberMe() != null && request.getRememberMe()
                ? refreshTokenExpiry * 2 // Double expiry for "remember me"
                : refreshTokenExpiry;

        saveRefreshToken(user, session, refreshTokenValue, expirySeconds, ipAddress, userAgent);

        // Update user's last login timestamp
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        logger.info(MSG_LOGIN_SUCCESS, Map.of(
                "userId", user.getId().toString(),
                "username", user.getUsername(),
                "sessionId", session.getSessionId().toString()));

        // Build response
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(accessTokenExpiry)
                .userId(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(displayName)
                .sessionId(session.getSessionId().toString())
                .build();
    }

    /**
     * Refreshes access token using a refresh token.
     *
     * @param request The refresh token request
     * @return RefreshTokenResponse with new access token and optionally new refresh
     *         token
     * @throws AuthenticationException if refresh token is invalid
     */
    @Transactional
    public RefreshTokenResponse refreshAccessToken(RefreshTokenRequest request) {
        logger.info(MSG_TOKEN_REFRESH_REQUEST);

        String refreshTokenValue = request.getRefreshToken();

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshTokenValue)) {
            logger.warn(MSG_TOKEN_REFRESH_FAILED_INVALID);
            throw new AuthenticationException("Invalid or expired refresh token");
        }

        // Extract user ID and session ID from token
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshTokenValue);
        UUID sessionId = jwtTokenProvider.getSessionIdFromToken(refreshTokenValue);

        // Find refresh token in database
        String tokenHash = hashToken(refreshTokenValue);
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    logger.warn(MSG_TOKEN_REFRESH_FAILED_NOT_FOUND);
                    return new AuthenticationException("Invalid or expired refresh token");
                });

        // Check if token is revoked
        if (refreshToken.isRevoked()) {
            logger.warn(MSG_TOKEN_REFRESH_FAILED_REVOKED, Map.of(
                    "tokenId", refreshToken.getId().toString()));
            throw new AuthenticationException("Invalid or expired refresh token");
        }

        // Check if token is expired
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            logger.warn(MSG_TOKEN_REFRESH_FAILED_EXPIRED, Map.of(
                    "tokenId", refreshToken.getId().toString()));
            throw new AuthenticationException("Refresh token has expired");
        }

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // Check if account is still active
        if (!Boolean.TRUE.equals(user.getEnabled()) || Boolean.TRUE.equals(user.getLocked())) {
            logger.warn(MSG_TOKEN_REFRESH_FAILED_ACCOUNT_INACTIVE, Map.of(
                    "userId", user.getId().toString()));
            throw new AuthenticationException("Account is not active");
        }

        // Update session last activity
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new AuthenticationException("Session not found"));

        session.setLastAccessedAt(Instant.now());
        sessionRepository.save(session);

        // Generate new access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(user, session.getSessionId());

        // Update last used timestamp for the refresh token
        refreshTokenDao.updateLastUsed(refreshToken.getId());

        logger.info(MSG_TOKEN_REFRESHED_SUCCESS_LOG, Map.of(
                "userId", user.getId().toString(),
                "sessionId", sessionId.toString()));

        // Build response (no token rotation for now)
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .expiresIn(accessTokenExpiry)
                .build();
    }

    /**
     * Logs out a user by revoking the refresh token and invalidating the session.
     *
     * @param refreshTokenValue The refresh token to revoke
     */
    @Transactional
    public void logout(String refreshTokenValue) {
        logger.info(MSG_LOGOUT_REQUEST);

        try {
            // Find and revoke refresh token
            String tokenHash = hashToken(refreshTokenValue);
            RefreshToken refreshToken = refreshTokenRepository
                    .findByTokenHash(tokenHash)
                    .orElse(null);

            if (refreshToken != null && !refreshToken.isRevoked()) {
                refreshToken.setRevoked(true);
                refreshToken.setRevokedAt(Instant.now());
                refreshTokenRepository.save(refreshToken);

                // Invalidate session
                Session session = sessionRepository.findBySessionId(refreshToken.getSessionId())
                        .orElse(null);

                if (session != null && !session.isRevoked()) {
                    session.setRevoked(true);
                    session.setRevokedAt(Instant.now());
                    session.setRevocationReason("User logout");
                    sessionRepository.save(session);

                    logger.info(MSG_LOGOUT_SUCCESS, Map.of(
                            "userId", refreshToken.getUser().getId().toString(),
                            "sessionId", session.getSessionId().toString()));
                }
            }

            // Blacklist tokens
            if (jwtTokenProvider.validateToken(refreshTokenValue)) {
                jwtTokenProvider.blacklistToken(refreshTokenValue);
            }
        } catch (Exception e) {
            logger.error(MSG_LOGOUT_ERROR_LOG, Map.of("error", e.getMessage()));
            // Don't throw exception - logout should be idempotent
        }
    }

    /**
     * Logs out all sessions for a user.
     *
     * @param userId The user ID
     */
    @Transactional
    public void logoutAll(UUID userId) {
        logger.info(MSG_LOGOUT_ALL_SESSIONS, Map.of("userId", userId.toString()));

        // Revoke all refresh tokens for user
        refreshTokenDao.revokeAllTokensForUser(userId);

        // Invalidate all active sessions using the DAO
        sessionDao.revokeAllSessionsForUser(userId, "User logout all");

        logger.info(MSG_ALL_SESSIONS_LOGGED_OUT, Map.of("userId", userId.toString()));
    }

    /**
     * Handles failed login attempt by incrementing counter and locking account if
     * needed.
     *
     * @param user The user who failed to login
     */
    private void handleFailedLogin(User user) {
        // This method would increment a failed login counter
        // For now, we'll log it - full implementation requires additional User entity
        // fields
        logger.warn(MSG_FAILED_LOGIN_ATTEMPT_RECORDED, Map.of(
                "userId", user.getId().toString(),
                "username", user.getUsername()));

        // TODO: Implement failed login tracking when User entity has
        // failedLoginAttempts field
        // int attempts = user.getFailedLoginAttempts() + 1;
        // user.setFailedLoginAttempts(attempts);
        //
        // if (attempts >= maxFailedLoginAttempts) {
        // user.setLocked(true);
        // user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(accountLockoutDurationMinutes)));
        // logger.warn("Account locked due to failed login attempts", Map.of(
        // "userId", user.getId().toString(),
        // "attempts", attempts
        // ));
        // }
        //
        // userRepository.save(user);
    }

    /**
     * Resets failed login attempts counter on successful login.
     *
     * @param user The user who successfully logged in
     */
    private void resetFailedLoginAttempts(User user) {
        // TODO: Implement when User entity has failedLoginAttempts field
        // if (user.getFailedLoginAttempts() > 0) {
        // user.setFailedLoginAttempts(0);
        // user.setLockedUntil(null);
        // userRepository.save(user);
        // }
    }

    /**
     * Creates a new session for the user.
     *
     * @param user      The user
     * @param ipAddress The IP address
     * @param userAgent The user agent
     * @return The created session
     */
    private Session createSession(User user, String ipAddress, String userAgent) {
        Session session = new Session();
        session.setUser(user);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);

        // Generate device fingerprint from user agent and IP
        String deviceFingerprint = generateDeviceFingerprint(userAgent, ipAddress);
        session.setDeviceFingerprint(deviceFingerprint);

        session.setRevoked(false);
        session.setLastAccessedAt(Instant.now());

        // Set session expiry (30 days from now)
        session.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));

        return sessionRepository.save(session);
    }

    /**
     * Generates a device fingerprint from user agent and IP address.
     *
     * @param userAgent The user agent string
     * @param ipAddress The IP address
     * @return SHA-256 hash of the combined values
     */
    private String generateDeviceFingerprint(String userAgent, String ipAddress) {
        String combined = (userAgent != null ? userAgent : "") + "|" + (ipAddress != null ? ipAddress : "");
        return hashToken(combined);
    }

    /**
     * Saves a refresh token to the database.
     *
     * @param user          The user
     * @param session       The session
     * @param tokenValue    The token value
     * @param expirySeconds The expiry duration in seconds
     * @param ipAddress     The IP address
     * @param userAgent     The user agent
     */
    private void saveRefreshToken(User user, Session session, String tokenValue, long expirySeconds,
            String ipAddress, String userAgent) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setSessionId(session.getSessionId());
        refreshToken.setTokenHash(hashToken(tokenValue));
        refreshToken.setExpiresAt(Instant.now().plus(Duration.ofSeconds(expirySeconds)));
        refreshToken.setRevoked(false);
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);

        // Set device info (optional, can be extracted from user agent)
        String deviceInfo = userAgent != null && userAgent.length() > 200
                ? userAgent.substring(0, 200)
                : userAgent;
        refreshToken.setDeviceInfo(deviceInfo);

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Extracts IP address from HTTP request (handles X-Forwarded-For header).
     *
     * @param request The HTTP request
     * @return The IP address
     */
    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Hashes a token using SHA-256 for secure storage.
     *
     * @param token The token to hash
     * @return The hashed token
     */
    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
