package com.example.Auth.application.service;

import com.example.Auth.api.dto.UserDTO;
import com.example.Auth.api.dto.request.LoginRequest;
import com.example.Auth.api.dto.request.RegisterRequest;
import com.example.Auth.api.dto.response.AuthTokenResponse;
import com.example.Auth.infrastructure.persistence.document.RefreshTokenDocument;
import com.example.Auth.infrastructure.persistence.document.TokenBlacklistDocument;
import com.example.Auth.infrastructure.persistence.document.UserDocument;
import com.example.Auth.infrastructure.persistence.repository.RefreshTokenRepository;
import com.example.Auth.infrastructure.persistence.repository.TokenBlacklistRepository;
import com.example.Auth.infrastructure.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthTokenResponse register(RegisterRequest request) {
        log.info("🔍 Checking if user exists with email: {}", request.getEmail());
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("⚠️ User already exists with email: {}", request.getEmail());
            throw new RuntimeException("User already exists with email: " + request.getEmail());
        }

        log.info("✅ Email available, proceeding with registration");
        // Create new user with hashed password
        UserDocument user = UserDocument.builder()
                .email(request.getEmail())
                .displayName(request.getDisplayName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status("ACTIVE")
                .roles(Set.of("USER"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        log.info("💾 Saving user to MongoDB: {}", request.getEmail());
        // Save user to database
        user = userRepository.save(user);
        log.info("✅ User saved successfully with ID: {}", user.getId());

        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        log.info("🎫 JWT tokens generated for user: {}", request.getEmail());

        // Store refresh token in database
        RefreshTokenDocument refreshTokenDoc = RefreshTokenDocument.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenExpirySeconds()))
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(refreshTokenDoc);
        log.info("💾 Refresh token stored for user: {}", user.getId());

        // Return response with user data
        return AuthTokenResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserDTO(user))
                .build();
    }

    public AuthTokenResponse login(LoginRequest request) {
        log.info("🔐 Login attempt for email: {}", request.getEmail());
        // Find user by email
        UserDocument user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("❌ User not found with email: {}", request.getEmail());
                    return new RuntimeException("User not found with email: " + request.getEmail());
                });

        log.info("✅ User found, verifying password");
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("❌ Invalid password for user: {}", request.getEmail());
            throw new RuntimeException("Invalid password");
        }

        log.info("✅ Password verified, checking user status");
        // Check if user is active
        if (!"ACTIVE".equals(user.getStatus())) {
            log.warn("❌ User account is not active for email: {}", request.getEmail());
            throw new RuntimeException("User account is not active");
        }

        log.info("✅ User active, generating JWT tokens");
        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // Store refresh token in database
        RefreshTokenDocument refreshTokenDoc = RefreshTokenDocument.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenExpirySeconds()))
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(refreshTokenDoc);
        log.info("💾 Refresh token stored for user: {}", user.getId());

        log.info("✅ Login successful for email: {}", request.getEmail());
        // Return response with user data
        return AuthTokenResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserDTO(user))
                .build();
    }

    private UserDTO mapToUserDTO(UserDocument user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }

    public UserDTO getUserById(String userId) {
        log.info("📋 Fetching user profile for userId: {}", userId);
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("❌ User not found with id: {}", userId);
                    return new RuntimeException("User not found");
                });
        log.info("✅ User found: {}", user.getEmail());
        return mapToUserDTO(user);
    }

    public UserDTO updateUser(String userId, UserDTO updateRequest) {
        log.info("📝 Updating user profile for userId: {}", userId);
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("❌ User not found with id: {}", userId);
                    return new RuntimeException("User not found");
                });

        // Update displayName if provided
        if (updateRequest.getDisplayName() != null && !updateRequest.getDisplayName().isBlank()) {
            user.setDisplayName(updateRequest.getDisplayName());
            log.info("✏️ Updated displayName to: {}", updateRequest.getDisplayName());
        }

        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);
        log.info("✅ User profile updated successfully");

        return mapToUserDTO(user);
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthTokenResponse refreshAccessToken(String refreshToken) {
        log.info("🔄 Attempting to refresh access token");

        // Check if token is in blacklist
        if (tokenBlacklistRepository.existsByToken(refreshToken)) {
            log.warn("❌ Refresh token has been revoked (in blacklist)");
            throw new RuntimeException("Refresh token has been revoked");
        }

        // Find refresh token in database
        RefreshTokenDocument refreshTokenDoc = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("❌ Refresh token not found or invalid");
                    return new RuntimeException("Refresh token not found or invalid");
                });

        // Check if token is expired
        if (refreshTokenDoc.isExpired()) {
            log.warn("❌ Refresh token has expired");
            throw new RuntimeException("Refresh token has expired");
        }

        // Check if token is revoked
        if (refreshTokenDoc.isRevoked()) {
            log.warn("❌ Refresh token has been revoked");
            throw new RuntimeException("Refresh token has been revoked");
        }

        log.info("✅ Refresh token validated, generating new access token for userId: {}", refreshTokenDoc.getUserId());

        // Fetch user details
        UserDocument user = userRepository.findById(refreshTokenDoc.getUserId())
                .orElseThrow(() -> {
                    log.warn("❌ User not found with id: {}", refreshTokenDoc.getUserId());
                    return new RuntimeException("User not found");
                });

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        log.info("🎫 New access token generated");

        // Optionally: Generate new refresh token for rotation (recommended for security)
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());
        RefreshTokenDocument newRefreshTokenDoc = RefreshTokenDocument.builder()
                .userId(user.getId())
                .token(newRefreshToken)
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenExpirySeconds()))
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(newRefreshTokenDoc);

        // Revoke old refresh token
        refreshTokenDoc.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshTokenDoc);
        log.info("🔒 Old refresh token revoked (token rotation)");

        log.info("✅ Token refresh successful");
        return AuthTokenResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(mapToUserDTO(user))
                .build();
    }

    /**
     * Logout user by adding token to blacklist
     */
    public void logout(String token, Instant expiresAt) {
        log.info("🚪 Logout request - adding token to blacklist");

        // Check if already blacklisted
        if (tokenBlacklistRepository.existsByToken(token)) {
            log.warn("⚠️ Token already blacklisted");
            return;
        }

        // Extract userId from token if available
        String userId = null;
        try {
            // We'll get userId from the request context instead
            userId = "unknown";
        } catch (Exception e) {
            log.debug("Could not extract userId from token");
        }

        // Add to blacklist
        TokenBlacklistDocument blacklistDoc = TokenBlacklistDocument.builder()
                .token(token)
                .userId(userId)
                .expiresAt(expiresAt)
                .blacklistedAt(Instant.now())
                .reason("LOGOUT")
                .build();

        tokenBlacklistRepository.save(blacklistDoc);
        log.info("✅ Token added to blacklist, will auto-expire on: {}", expiresAt);
    }
}
