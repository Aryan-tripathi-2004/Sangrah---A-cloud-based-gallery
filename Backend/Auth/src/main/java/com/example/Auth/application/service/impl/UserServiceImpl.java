package com.example.Auth.application.service.impl;

import com.example.Auth.api.dto.UserDTO;
import com.example.Auth.api.dto.request.UpdateProfileRequest;
import com.example.Auth.api.dto.response.UserWrapperResponse;
import com.example.Auth.application.service.interfaces.IUserService;
import com.example.Auth.infrastructure.client.MediaServiceClient;
import com.example.Auth.infrastructure.client.dto.MediaUploadResponse;
import com.example.Auth.infrastructure.mapper.AuthMapper;
import com.example.Auth.infrastructure.persistence.document.UserDocument;
import com.example.Auth.infrastructure.persistence.repository.RefreshTokenRepository;
import com.example.Auth.infrastructure.persistence.repository.UserRepository;
import com.example.Auth.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthMapper authMapper;
    private final MediaServiceClient mediaServiceClient;

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
    @Transactional
    public UserDTO updateUser(String userId, UpdateProfileRequest updateRequest) {
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

        if (updateRequest.bio() != null) {
            user.setBio(updateRequest.bio().trim());
            log.info("✏️ Updated bio to: {}", updateRequest.bio());
        }

        if (updateRequest.avatarUrl() != null && !updateRequest.avatarUrl().isBlank()) {
            user.setAvatarUrl(updateRequest.avatarUrl().trim());
            log.info("✏️ Updated avatarUrl to: {}", updateRequest.avatarUrl());
        }

        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);
        log.info("✅ User profile updated successfully");

        return authMapper.toUserDTO(user);
    }

    @Override
    @Transactional
    public UserDTO uploadAvatar(String userId, MultipartFile file) {
        log.info("📸 Uploading avatar photo for user: {}", userId);
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("❌ User not found with id: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file must not be empty");
        }

        MediaUploadResponse mediaResponse = mediaServiceClient.uploadMedia(
                file,
                "PROFILE_AVATAR",
                userId,
                userId
        );

        String mediaId = mediaResponse.resolvedMediaId();
        String avatarUrl = "/api/v1/media/" + mediaId;
        user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);

        log.info("✅ Avatar updated successfully for user {}: {}", userId, avatarUrl);
        return authMapper.toUserDTO(user);
    }

    @Override
    @Transactional
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
    public UserWrapperResponse getUserWrapperById(String userId) {
        log.info("🔍 Fetching user wrapper by userId: {}", userId);
        UserDTO user = getUserById(userId);
        return new UserWrapperResponse(user, "success", null);
    }

    @Override
    public UserWrapperResponse getUserWrapperByEmail(String email) {
        log.info("🔍 Fetching user wrapper by email: {}", email);
        UserDTO user = getUserByEmail(email);
        return new UserWrapperResponse(user, "success", null);
    }

    @Override
    public UserWrapperResponse findUserWrapper(String id, String email) {
        log.info("🔍 Finding user wrapper | id: {}, email: {}", id, email);
        if (email != null && !email.isBlank()) {
            return getUserWrapperByEmail(email);
        }
        if (id != null && !id.isBlank()) {
            return getUserWrapperById(id);
        }
        throw new IllegalArgumentException("Either 'id' or 'email' must be provided");
    }
}
