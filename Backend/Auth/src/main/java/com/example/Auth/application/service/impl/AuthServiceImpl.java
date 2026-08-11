package com.example.Auth.application.service.impl;

import com.example.Auth.api.dto.UserDTO;
import com.example.Auth.api.dto.request.LoginRequest;
import com.example.Auth.api.dto.request.RegisterRequest;
import com.example.Auth.api.dto.response.AuthTokenResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    @Override
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

        log.info("✅ User active, generating JWT tokens");
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

    @Override
    public UserDTO getUserById(String userId) {
        log.info("📋 Fetching user profile for userId: {}", userId);
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("❌ User not found with id: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });
        log.info("✅ User found: {}", user.getEmail());
        return authMapper.toUserDTO(user);
    }

    @Override
    public UserDTO getUserByEmail(String email) {
        log.info("📋 Fetching user profile for email: {}", email);
        UserDocument user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("❌ User not found with email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });
        log.info("✅ User found with email: {}", email);
        return authMapper.toUserDTO(user);
    }

    @Override
    public UserDTO updateUser(String userId, UserDTO updateRequest) {
        log.info("📝 Updating user profile for userId: {}", userId);
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("❌ User not found with id: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });

        if (updateRequest.displayName() != null && !updateRequest.displayName().isBlank()) {
            user.setDisplayName(updateRequest.displayName());
            log.info("✏️ Updated displayName to: {}", updateRequest.displayName());
        }

        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);
        log.info("✅ User profile updated successfully");

        return authMapper.toUserDTO(user);
    }

    @Override
    public void deleteUser(String userId) {
        log.info("🗑️ Deleting user account for userId: {}", userId);
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("❌ User not found with id: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });

        refreshTokenRepository.deleteByUserId(userId);
        log.info("🔒 Refresh tokens revoked for userId: {}", userId);

        userRepository.delete(user);
        log.info("✅ User account deleted: {}", userId);
    }

    @Override
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
        refreshTokenRepository.save(refreshTokenDoc);
        log.info("🔒 Old refresh token revoked (token rotation)");

        log.info("✅ Token refresh successful");
        return new AuthTokenResponse(newAccessToken, newRefreshToken, authMapper.toUserDTO(user));
    }

    @Override
    public void logout(String token, Instant expiresAt) {
        log.info("🚪 Logout request - adding token to blacklist");

        if (tokenBlacklistRepository.existsByToken(token)) {
            log.warn("⚠️ Token already blacklisted");
            return;
        }

        TokenBlacklistDocument blacklistDoc = TokenBlacklistDocument.builder()
                .token(token)
                .userId("unknown")
                .expiresAt(expiresAt)
                .blacklistedAt(Instant.now())
                .reason(RevocationReason.LOGOUT)
                .build();

        tokenBlacklistRepository.save(blacklistDoc);
        log.info("✅ Token added to blacklist, will auto-expire on: {}", expiresAt);
    }
}
