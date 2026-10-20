package com.example.Auth.api.controller;

import com.example.Auth.api.dto.request.RefreshTokenRequest;
import com.example.Auth.api.dto.response.AuthTokenResponse;
import com.example.Auth.api.dto.response.TokenValidationResponse;
import com.example.Auth.application.service.interfaces.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/token")
@RequiredArgsConstructor
public class TokenController {

    private final IAuthService authService;

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("🔄 Token refresh request received");
        AuthTokenResponse response = authService.refreshAccessToken(request.refreshToken());
        log.info("✅ Token refreshed successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate access token")
    public ResponseEntity<TokenValidationResponse> validate(@RequestHeader("Authorization") String authHeader) {
        log.info("🔍 Token validation request");
        return ResponseEntity.ok(authService.validateToken(authHeader));
    }
}
