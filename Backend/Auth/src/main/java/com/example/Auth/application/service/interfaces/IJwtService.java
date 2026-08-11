package com.example.Auth.application.service.interfaces;

public interface IJwtService {
    String generateAccessToken(String userId, String email);
    String generateRefreshToken(String userId);
    long getRefreshTokenExpirySeconds();
}
