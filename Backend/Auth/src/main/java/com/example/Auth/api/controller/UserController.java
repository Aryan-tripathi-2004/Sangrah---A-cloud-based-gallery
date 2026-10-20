package com.example.Auth.api.controller;

import com.example.Auth.api.annotation.CurrentUserId;
import com.example.Auth.api.dto.UserDTO;
import com.example.Auth.api.dto.request.UpdateProfileRequest;
import com.example.Auth.api.dto.response.UserWrapperResponse;
import com.example.Auth.application.service.interfaces.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDTO> getProfile(@CurrentUserId String userId) {
        log.info("  Fetching profile for user: {}", userId);
        UserDTO user = userService.getUserById(userId);
        log.info("  Profile fetched successfully");
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user profile")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDTO> updateProfile(
            @CurrentUserId String userId,
            @Valid @RequestBody UpdateProfileRequest updateRequest) {
        log.info("  Updating profile for user: {}", userId);
        
        // Convert request DTO to UserDTO internally for service layer 
        // to avoid cascading changes too deeply if unnecessary, 
        // but ideally update IAuthService method signature. Let's assume we update the service too.
        UserDTO updatedUser = userService.updateUser(userId, updateRequest);
        
        log.info("  Profile updated successfully");
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload current user profile avatar")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UserDTO> uploadAvatar(
            @CurrentUserId String userId,
            @RequestParam("file") MultipartFile file) {
        log.info("📸 Avatar upload request for user: {}", userId);
        UserDTO user = userService.uploadAvatar(userId, file);
        log.info("✅ Avatar uploaded successfully for user: {}", userId);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/profile")
    @Operation(summary = "Delete current user account")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Void> deleteAccount(@CurrentUserId String userId) {
        log.info("  Deleting account for user: {}", userId);
        userService.deleteUser(userId);
        log.info("  Account deleted successfully");
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Find user by identifier")
    public ResponseEntity<UserWrapperResponse> findUser(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String email) {
        
        log.info("  Fetching user data for id: {} or email: {}", id, email);
        return ResponseEntity.ok(userService.findUserWrapper(id, email));
    }
}