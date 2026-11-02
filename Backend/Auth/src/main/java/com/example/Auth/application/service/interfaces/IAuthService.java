package com.example.Auth.application.service.interfaces;

import com.example.Auth.api.dto.request.LoginRequest;
import com.example.Auth.api.dto.request.RegisterRequest;
import com.example.Auth.api.dto.response.AuthTokenResponse;
import com.example.Auth.api.dto.response.MessageResponse;
import com.example.Auth.api.dto.response.TokenValidationResponse;

public interface IAuthService {
    AuthTokenResponse register(RegisterRequest request);
    AuthTokenResponse login(LoginRequest request);
    AuthTokenResponse refreshAccessToken(String refreshToken);
    MessageResponse logout(String authHeader, String userId);
    TokenValidationResponse validateToken(String authHeader);
}
