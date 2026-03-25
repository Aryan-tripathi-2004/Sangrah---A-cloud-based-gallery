package com.example.Auth.api.dto.response;

import com.example.Auth.api.dto.UserDTO;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthTokenResponse {
    private String token;  // Changed from accessToken to match frontend
    private String refreshToken;
    private UserDTO user;  // Add user data
}
