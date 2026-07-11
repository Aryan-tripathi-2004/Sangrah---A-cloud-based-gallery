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


    /**
     * Delete curent user's account
     * API Gateway provides X-User-Id header after JWT validation.  
    */

   @DeleteMapping("/profile")
   @Operation(summary = "Delete current user account")
   @SecurityRequirement(name = "Bearer Authentication") 

   public ResponseEntity<Void> deleteAccount(HttpServletRequest request){
    try{
        String userId = request.getHaeder("X-User-Id");

        if (userId == null || userId.isBlank()){
            log.warn("❌ No X-User-Id header found");
            return ResponseEntity.status(401).build();
        }

        log.info("🗑️ Deleting account for user: {}", userId);
        authService.deleteUser(userId);
        log.info("✅ Account deleted successfully");
        
        return ResponseEntity.noContent().build();
    } catch (Exception e){
        log.error("❌ Error deleting account: {}", e.getMessage(), e);
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
    public ResponseEntity<Map<String, Object>> byId(@PathVariable String userId) {
        try {
            log.info("👤 Fetching user data for userId: {}", userId);
            UserDTO user = authService.getUserById(userId);
            log.info("✅ User found: {}", user.getEmail());

            // Return UserDTO wrapped in data object for consistency with API responses
            return ResponseEntity.ok(Map.of(
                    "data", user,
                    "status", "success"
            ));
        } catch (Exception e) {
            log.error("❌ Error fetching user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "User not found"
            ));
        }
    }

    /**
     * Get user by email (for collaborator lookup)
     * GET /api/v1/users/by-email/{email}
     */
    @GetMapping("/by-email/{email}")
    @Operation(summary = "Get user by email")
    public ResponseEntity<Map<String, Object>> byEmail(@PathVariable String email) {
        try {
            log.info("👤 Fetching user data for email: {}", email);
            UserDTO user = authService.getUserByEmail(email);
            log.info("✅ User found with email: {}", email);

            // Return UserDTO wrapped in data object for consistency with API responses
            return ResponseEntity.ok(Map.of(
                    "data", user,
                    "userId", user.getId(),
                    "email", user.getEmail(),
                    "displayName", user.getDisplayName(),
                    "status", "success"
            ));
        } catch (Exception e) {
            log.error("❌ Error fetching user with email {}: {}", email, e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                    "status", "error",
                    "message", "User not found with email: " + email
            ));
        }
    }

     
}
