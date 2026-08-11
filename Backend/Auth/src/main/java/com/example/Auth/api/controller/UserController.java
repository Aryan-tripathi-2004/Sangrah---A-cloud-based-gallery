package com.example.Auth.api.controller;

import com.example.Auth.api.annotation.CurrentUserId;
import com.example.Auth.api.dto.UserDTO;
import com.example.Auth.api.dto.response.UserWrapperResponse;
import com.example.Auth.application.service.interfaces.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final IAuthService authService;

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDTO> getProfile(@CurrentUserId String userId) {
        log.info("  Fetching profile for user: {}", userId);
        UserDTO user = authService.getUserById(userId);
        log.info("  Profile fetched successfully");
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user profile")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDTO> updateProfile(
            @CurrentUserId String userId,
            @RequestBody UserDTO updateRequest) {
        log.info("  Updating profile for user: {}", userId);
        UserDTO updatedUser = authService.updateUser(userId, updateRequest);
        log.info("  Profile updated successfully");
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/profile")
    @Operation(summary = "Delete current user account")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Void> deleteAccount(@CurrentUserId String userId) {
        log.info("  Deleting account for user: {}", userId);
        authService.deleteUser(userId);
        log.info("  Account deleted successfully");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by id")
    public ResponseEntity<UserWrapperResponse> byId(@PathVariable String userId) {
        log.info("  Fetching user data for userId: {}", userId);
        UserDTO user = authService.getUserById(userId);
        log.info("  User found: {}", user.email());
        return ResponseEntity.ok(new UserWrapperResponse(user, "success", null));
    }

    @GetMapping("/by-email/{email}")
    @Operation(summary = "Get user by email")
    public ResponseEntity<UserWrapperResponse> byEmail(@PathVariable String email) {
        log.info("  Fetching user data for email: {}", email);
        UserDTO user = authService.getUserByEmail(email);
        log.info("  User found with email: {}", email);
        return ResponseEntity.ok(new UserWrapperResponse(user, "success", null));
    }
}