package com.example.Auth.api.controller;

import com.example.Auth.api.dto.UserDTO;
import com.example.Auth.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    /**
     * Get current user's profile.
     * API Gateway provides X-User-Id header after JWT validation.
     */
    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDTO> getProfile(HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");

            if (userId == null || userId.isBlank()) {
                log.warn("❌ No X-User-Id header found");
                return ResponseEntity.status(401).build();
            }

            log.info("📋 Fetching profile for user: {}", userId);
            UserDTO user = authService.getUserById(userId);
            log.info("✅ Profile fetched successfully");

            return ResponseEntity.ok(user);

        } catch (Exception e) {
            log.error("❌ Error fetching profile: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Update current user's profile.
     * API Gateway provides X-User-Id header after JWT validation.
     */
    @PutMapping("/profile")
    @Operation(summary = "Update current user profile")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDTO> updateProfile(
            HttpServletRequest request,
            @RequestBody UserDTO updateRequest) {
        try {
            String userId = request.getHeader("X-User-Id");

            if (userId == null || userId.isBlank()) {
                log.warn("❌ No X-User-Id header found");
                return ResponseEntity.status(401).build();
            }

            log.info("✏️ Updating profile for user: {}", userId);
            UserDTO updatedUser = authService.updateUser(userId, updateRequest);
            log.info("✅ Profile updated successfully");

            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            log.error("❌ Error updating profile: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user")
    public ResponseEntity<Map<String, String>> me() {
        return ResponseEntity.ok(Map.of("userId", "me", "displayName", "Demo User"));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update current user")
    public ResponseEntity<Map<String, String>> updateMe(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by id")
    public ResponseEntity<Map<String, String>> byId(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of("userId", userId));
    }
}
