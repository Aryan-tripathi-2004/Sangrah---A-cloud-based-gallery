package com.example.Auth.api.controller;

import com.example.Auth.api.dto.request.LoginRequest;
import com.example.Auth.api.dto.request.RegisterRequest;
import com.example.Auth.api.dto.response.AuthTokenResponse;
import com.example.Auth.api.dto.response.MessageResponse;
import com.example.Auth.application.service.interfaces.IAuthService;
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

    private final IAuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a user")
    public ResponseEntity<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("📝 Register request for email: {}", request.email());
        AuthTokenResponse response = authService.register(request);
        log.info("✅ Registration successful for email: {}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("🔐 Login request for email: {}", request.email());
        AuthTokenResponse response = authService.login(request);
        log.info("✅ Login successful for email: {}", request.email());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and revoke token")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "${security.header.user-id:X-User-Id}", required = false) String userId) {
        log.info("🚪 Logout request received");
        MessageResponse response = authService.logout(authHeader, userId);
        log.info("✅ Logout successful, token revoked");
        return ResponseEntity.ok(response);
    }
}
