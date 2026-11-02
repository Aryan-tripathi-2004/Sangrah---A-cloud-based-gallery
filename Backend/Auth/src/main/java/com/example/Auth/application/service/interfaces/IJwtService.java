package com.example.Auth.application.service.interfaces;

import com.example.Auth.shared.enums.Role;
import java.util.Set;

public interface IJwtService {
    String generateAccessToken(String userId, String email);
    String generateAccessToken(String userId, String email, Set<Role> roles);
    String generateRefreshToken(String userId);
    long getRefreshTokenExpirySeconds();
    String extractUserId(String token);
    boolean isTokenValid(String token);
}
