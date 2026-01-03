package com.example.Auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Response DTO for token refresh operation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RefreshTokenResponse {

    private String accessToken;
    private String refreshToken; // New refresh token if rotation is enabled
    private String tokenType = "Bearer";
    private Long expiresIn; // Access token expiry in seconds
    private Instant refreshedAt;

    // Constructors

    public RefreshTokenResponse() {
        this.refreshedAt = Instant.now();
    }

    public RefreshTokenResponse(String accessToken, String refreshToken, Long expiresIn) {
        this();
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    // Builder pattern

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String accessToken;
        private String refreshToken;
        private Long expiresIn;

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder expiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public RefreshTokenResponse build() {
            RefreshTokenResponse response = new RefreshTokenResponse();
            response.accessToken = this.accessToken;
            response.refreshToken = this.refreshToken;
            response.expiresIn = this.expiresIn;
            return response;
        }
    }

    // Getters and Setters

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Instant getRefreshedAt() {
        return refreshedAt;
    }

    public void setRefreshedAt(Instant refreshedAt) {
        this.refreshedAt = refreshedAt;
    }

    @Override
    public String toString() {
        return "RefreshTokenResponse{" +
                "tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", refreshedAt=" + refreshedAt +
                '}';
    }
}
