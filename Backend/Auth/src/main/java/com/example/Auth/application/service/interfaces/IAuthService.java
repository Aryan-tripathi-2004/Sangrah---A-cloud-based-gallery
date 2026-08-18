package com.example.Auth.application.service.interfaces;

import com.example.Auth.api.dto.UserDTO;
import com.example.Auth.api.dto.request.LoginRequest;
import com.example.Auth.api.dto.request.RegisterRequest;
import com.example.Auth.api.dto.response.AuthTokenResponse;

import java.time.Instant;

public interface IAuthService {
    AuthTokenResponse register(RegisterRequest request);
    AuthTokenResponse login(LoginRequest request);
    UserDTO getUserById(String userId);
    UserDTO getUserByEmail(String email);
    UserDTO updateUser(String userId, UserDTO updateRequest);
    void deleteUser(String userId);
    AuthTokenResponse refreshAccessToken(String refreshToken);
    void logout(String token, Instant expiresAt);
}
