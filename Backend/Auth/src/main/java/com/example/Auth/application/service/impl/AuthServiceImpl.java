package com.example.Auth.application.service.impl;

import com.example.Auth.api.dto.request.LoginRequest;
import com.example.Auth.api.dto.request.RegisterRequest;
import com.example.Auth.api.dto.response.AuthTokenResponse;
import com.example.Auth.api.dto.response.MessageResponse;
import com.example.Auth.api.dto.response.TokenValidationResponse;
import com.example.Auth.application.service.interfaces.IAuthService;
import com.example.Auth.application.service.interfaces.IJwtService;
import com.example.Auth.infrastructure.mapper.AuthMapper;
import com.example.Auth.infrastructure.persistence.document.RefreshTokenDocument;
import com.example.Auth.infrastructure.persistence.document.TokenBlacklistDocument;
import com.example.Auth.infrastructure.persistence.document.UserDocument;
import com.example.Auth.infrastructure.persistence.repository.RefreshTokenRepository;
import com.example.Auth.infrastructure.persistence.repository.TokenBlacklistRepository;
import com.example.Auth.infrastructure.persistence.repository.UserRepository;
import com.example.Auth.shared.enums.RevocationReason;
import com.example.Auth.shared.enums.Role;
import com.example.Auth.shared.enums.UserStatus;
import com.example.Auth.shared.exception.AuthenticationException;
import com.example.Auth.shared.exception.DomainValidationException;
import com.example.Auth.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {
    private final IJwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;

    @Value("${jwt.access-token-expiration-seconds}")
    private long accessTokenExpirySeconds;

    @Value("${auth.session.max-active}")
    private int maxActiveSessions;

    @Override
    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        log.info("🔍 Checking if user exists with email: {}", request.email());
        if (userRepository.existsByEmail(request.email())) {
            log.warn("⚠️ User already exists with email: {}", request.email());
            throw new DomainValidationException("User already exists with email: " + request.email());
        }

        log.info("✅ Email available, proceeding with registration");
        UserDocument user = UserDocument.builder()
                .email(request.email())
                .displayName(request.displayName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.USER))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        log.info("💾 Saving user to MongoDB: {}", request.email());
        user = userRepository.save(user);
        log.info("✅ User saved successfully with ID: {}", user.getId());

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        log.info("🎫 JWT tokens generated for user: {}", request.email());

        RefreshTokenDocument refreshTokenDoc = RefreshTokenDocument.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenExpirySeconds()))
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(refreshTokenDoc);
        log.info("💾 Refresh token stored for user: {}", user.getId());

        return new AuthTokenResponse(accessToken, refreshToken, authMapper.toUserDTO(user));
    }

    @Override
    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        log.info("🔐 Login attempt for email: {}", request.email());
        UserDocument user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("❌ User not found with email: {}", request.email());
                    return new AuthenticationException("Invalid email or password");
                });

        log.info("✅ User found, verifying password");
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("❌ Invalid password for user: {}", request.email());
            throw new AuthenticationException("Invalid email or password");
        }

        log.info("✅ Password verified, checking user status");
        if (UserStatus.ACTIVE != user.getStatus()) {
            log.warn("❌ User account is not active for email: {}", request.email());
            throw new AuthenticationException("User account is not active");
        }

        log.info("✅ User active, managing session limits");
        evictExcessActiveSessions(user.getId());

        log.info("✅ Generating JWT tokens");
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        RefreshTokenDocument refreshTokenDoc = RefreshTokenDocument.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenExpirySeconds()))
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(refreshTokenDoc);
        log.info("💾 Refresh token stored for user: {}", user.getId());

        log.info("✅ Login successful for email: {}", request.email());
        return new AuthTokenResponse(accessToken, refreshToken, authMapper.toUserDTO(user));
    }

    /**
     * Evicts older active sessions if the user has reached or exceeded maxActiveSessions.
     * Prevents unbounded collection growth and session accumulation in MongoDB.
     */
    private void evictExcessActiveSessions(String userId) {
        try {
            long activeSessions = refreshTokenRepository.countByUserIdAndRevokedAtIsNull(userId);
            if (activeSessions >= maxActiveSessions) {
                long sessionsToRevoke = activeSessions - maxActiveSessions + 1;
                log.info("🧹 Active sessions ({}) reached limit ({}), evicting {} oldest session(s) for user: {}",
                        activeSessions, maxActiveSessions, sessionsToRevoke, userId);

                List<RefreshTokenDocument> oldestSessions = refreshTokenRepository
                        .findByUserIdAndRevokedAtIsNullOrderByCreatedAtAsc(userId);

                List<String> idsToDelete = oldestSessions.stream()
                        .limit(sessionsToRevoke)
                        .map(RefreshTokenDocument::getId)
                        .filter(id -> id != null && !id.isBlank())
                        .toList();

                if (!idsToDelete.isEmpty()) {
                    refreshTokenRepository.deleteAllById(idsToDelete);
                    log.info("🚪 Successfully evicted {} oldest active session(s) for user: {}", idsToDelete.size(), userId);
                }
            }
        } catch (Exception ex) {
            log.warn("⚠️ Failed to evict excess sessions for user: {}. Continuing login without disruption. Cause: {}",
                    userId, ex.getMessage(), ex);
        }
    }


    @Override
    @Transactional
    public AuthTokenResponse refreshAccessToken(String refreshToken) {
        log.info("🔄 Attempting to refresh access token");

        if (tokenBlacklistRepository.existsByToken(refreshToken)) {
            log.warn("❌ Refresh token has been revoked (in blacklist)");
            throw new AuthenticationException("Refresh token has been revoked");
        }

        RefreshTokenDocument refreshTokenDoc = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("❌ Refresh token not found or invalid");
                    return new AuthenticationException("Refresh token not found or invalid");
                });

        if (refreshTokenDoc.isExpired()) {
            log.warn("❌ Refresh token has expired");
            throw new AuthenticationException("Refresh token has expired");
        }

        if (refreshTokenDoc.isRevoked()) {
            // RFC 6819 Token Rotation Grace Period (30 seconds)
            // Allows near-simultaneous concurrent requests across multiple components or tabs without killing the session
            Instant graceCutoff = refreshTokenDoc.getRevokedAt() != null
                    ? refreshTokenDoc.getRevokedAt().plusSeconds(30)
                    : null;
            if (graceCutoff != null && Instant.now().isBefore(graceCutoff)
                    && refreshTokenDoc.getReplacedByToken() != null) {
                log.info("🔄 Refresh token was rotated within 30s grace window (concurrent refresh). Returning active token.");
                UserDocument user = userRepository.findById(refreshTokenDoc.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
                return new AuthTokenResponse(newAccessToken, refreshTokenDoc.getReplacedByToken(), authMapper.toUserDTO(user));
            }
            log.warn("❌ Refresh token has been revoked");
            throw new AuthenticationException("Refresh token has been revoked");
        }

        log.info("✅ Refresh token validated, generating new access token for userId: {}", refreshTokenDoc.getUserId());

        UserDocument user = userRepository.findById(refreshTokenDoc.getUserId())
                .orElseThrow(() -> {
                    log.warn("❌ User not found with id: {}", refreshTokenDoc.getUserId());
                    return new ResourceNotFoundException("User not found");
                });

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        log.info("🎫 New access token generated");

        String newRefreshToken = jwtService.generateRefreshToken(user.getId());
        RefreshTokenDocument newRefreshTokenDoc = RefreshTokenDocument.builder()
                .userId(user.getId())
                .token(newRefreshToken)
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenExpirySeconds()))
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(newRefreshTokenDoc);

        refreshTokenDoc.setRevokedAt(Instant.now());
        refreshTokenDoc.setReplacedByToken(newRefreshToken);
        refreshTokenRepository.save(refreshTokenDoc);
        log.info("🔒 Old refresh token revoked (token rotation) and linked to successor");

        log.info("✅ Token refresh successful");
        return new AuthTokenResponse(newAccessToken, newRefreshToken, authMapper.toUserDTO(user));
    }


    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    @Transactional
    public MessageResponse logout(String authHeader, String userId) {
        log.info("🚪 Logout request - adding token to blacklist and revoking refresh sessions");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationException("Invalid Authorization header format");
        }
        String token = authHeader.substring(BEARER_PREFIX.length());

        if (userId == null || userId.isBlank()) {
            try {
                userId = jwtService.extractUserId(token);
            } catch (Exception e) {
                log.warn("⚠️ Could not extract userId from token during logout: {}", e.getMessage());
            }
        }

        Instant expiresAt = Instant.now().plusSeconds(accessTokenExpirySeconds);

        if (tokenBlacklistRepository.existsByToken(token)) {
            log.warn("⚠️ Token already blacklisted");
        } else {
            TokenBlacklistDocument blacklistDoc = TokenBlacklistDocument.builder()
                    .token(token)
                    .userId(userId != null ? userId : "unknown")
                    .expiresAt(expiresAt)
                    .blacklistedAt(Instant.now())
                    .reason(RevocationReason.LOGOUT)
                    .build();

            tokenBlacklistRepository.save(blacklistDoc);
            log.info("✅ Token added to blacklist, will auto-expire on: {}", expiresAt);
        }

        if (userId != null && !userId.isBlank()) {
            refreshTokenRepository.deleteByUserId(userId);
            log.info("🔒 Active refresh tokens revoked for user: {}", userId);
        }

        return new MessageResponse("Logout successful", Instant.now().toString());
    }

    @Override
    public TokenValidationResponse validateToken(String authHeader) {
        log.info("🔍 Token validation request");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationException("Authorization header missing or invalid");
        }
        return new TokenValidationResponse(true);
    }
}
