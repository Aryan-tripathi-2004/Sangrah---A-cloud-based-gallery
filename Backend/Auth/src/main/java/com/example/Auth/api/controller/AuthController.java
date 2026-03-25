package com.example.Auth.api.controller;

import com.example.Auth.api.dto.request.LoginRequest;
import com.example.Auth.api.dto.request.RegisterRequest;
import com.example.Auth.api.dto.response.AuthTokenResponse;
import com.example.Auth.api.dto.response.ErrorResponse;
import com.example.Auth.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a user")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            log.info("📝 Register request for email: {}", request.getEmail());
            AuthTokenResponse response = authService.register(request);
            log.info("✅ Registration successful for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("❌ Registration error: {}", e.getMessage(), e);
            ErrorResponse error = ErrorResponse.builder()
                    .error("REGISTRATION_FAILED")
                    .message(e.getMessage())
                    .status(400)
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            log.error("❌ Unexpected error during registration: {}", e.getMessage(), e);
            ErrorResponse error = ErrorResponse.builder()
                    .error("INTERNAL_ERROR")
                    .message("An unexpected error occurred")
                    .status(500)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("🔐 Login request for email: {}", request.getEmail());
            AuthTokenResponse response = authService.login(request);
            log.info("✅ Login successful for email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("❌ Login error: {}", e.getMessage());
            ErrorResponse error = ErrorResponse.builder()
                    .error("LOGIN_FAILED")
                    .message(e.getMessage())
                    .status(401)
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            log.error("❌ Unexpected error during login: {}", e.getMessage(), e);
            ErrorResponse error = ErrorResponse.builder()
                    .error("INTERNAL_ERROR")
                    .message("An unexpected error occurred")
                    .status(500)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and revoke token")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("❌ Logout request without valid Authorization header");
                return ResponseEntity.badRequest().body(ErrorResponse.builder()
                        .error("INVALID_REQUEST")
                        .message("Authorization header required")
                        .status(400)
                        .build());
            }

            String token = authHeader.substring(7);
            log.info("🚪 Logout request for token");

            // Extract expiration from token (would need JWT parsing in production)
            java.time.Instant expiresAt = java.time.Instant.now().plusSeconds(3600);

            authService.logout(token, expiresAt);
            log.info("✅ Logout successful, token revoked");

            return ResponseEntity.ok(java.util.Map.of(
                    "message", "Logout successful",
                    "timestamp", java.time.Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("❌ Logout error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder()
                    .error("LOGOUT_FAILED")
                    .message("An error occurred during logout")
                    .status(500)
                    .build());
        }
    }
}
