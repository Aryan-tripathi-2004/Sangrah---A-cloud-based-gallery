package com.example.Auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import static com.example.Auth.util.SchemaConstants.*;

/**
 * Request DTO for refreshing access token.
 */
@Schema(description = SCHEMA_REFRESH_TOKEN_REQUEST_DESC)
public class RefreshTokenRequest {

    @Schema(description = SCHEMA_REFRESH_TOKEN_DESC, example = SCHEMA_REFRESH_TOKEN_EXAMPLE, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    // Constructors

    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // Getters and Setters

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public String toString() {
        return "RefreshTokenRequest{" +
                "refreshToken='[REDACTED]'" +
                '}';
    }
}
