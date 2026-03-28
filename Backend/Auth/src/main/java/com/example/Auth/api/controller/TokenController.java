package com.example.Auth.api.controller;

import com.example.Auth.api.dto.request.RefreshTokenRequest;
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
@RequestMapping("/api/v1/auth/token")
@RequiredArgsConstructor
public class TokenController {

    private final AuthService authService;

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            log.info("🔄 Token refresh request received");
            AuthTokenResponse response = authService.refreshAccessToken(request.getRefreshToken());
            log.info("✅ Token refreshed successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("❌ Token refresh error: {}", e.getMessage());
            ErrorResponse error = ErrorResponse.builder()
                    .error("TOKEN_REFRESH_FAILED")
                    .message(e.getMessage())
                    .status(401)
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            log.error("❌ Unexpected error during token refresh: {}", e.getMessage(), e);
            ErrorResponse error = ErrorResponse.builder()
                    .error("INTERNAL_ERROR")
                    .message("An unexpected error occurred")
                    .status(500)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate access token")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponse.builder()
                                .error("INVALID_TOKEN_FORMAT")
                                .message("Authorization header missing or invalid")
                                .status(401)
                                .build());
            }

            String token = authHeader.substring(7);
            log.info("🔍 Token validation request");

            // Token validation is done by API Gateway, if we reach here it's already validated
            return ResponseEntity.ok(java.util.Map.of("valid", true));
        } catch (Exception e) {
            log.error("❌ Token validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.builder()
                            .error("TOKEN_VALIDATION_FAILED")
                            .message(e.getMessage())
                            .status(401)
                            .build());
        }
    }
}
