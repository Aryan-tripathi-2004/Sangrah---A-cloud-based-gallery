package com.example.Auth.dto;

import com.example.Auth.common.logging.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.example.Auth.util.SchemaConstants.*;

/**
 * Request DTO for user login.
 * Supports login with either username or email.
 */
@Schema(description = SCHEMA_LOGIN_REQUEST_DESC)
public class LoginRequest {

    @Schema(description = SCHEMA_USERNAME_OR_EMAIL_DESC, example = SCHEMA_USERNAME_OR_EMAIL_EXAMPLE, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username or email is required")
    @Size(max = 255, message = "Username or email cannot exceed 255 characters")
    private String usernameOrEmail;

    @Schema(description = SCHEMA_USER_PASSWORD_DESC, example = SCHEMA_PASSWORD_EXAMPLE, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 128, message = "Password must be between 1 and 128 characters")
    @Sensitive(maskChar = '*', showFirst = 0, showLast = 0)
    private String password;

    /**
     * Optional device information for session tracking.
     */
    @Schema(description = SCHEMA_DEVICE_NAME_DESC, example = SCHEMA_DEVICE_NAME_EXAMPLE, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 200, message = "Device name cannot exceed 200 characters")
    private String deviceName;

    @Schema(description = SCHEMA_USER_AGENT_DESC, example = SCHEMA_USER_AGENT_EXAMPLE, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 500, message = "User agent cannot exceed 500 characters")
    private String userAgent;

    /**
     * Flag to indicate if this is a "remember me" login.
     * If true, refresh token will have extended expiry (e.g., 30 days).
     */
    @Schema(description = SCHEMA_REMEMBER_ME_DESC, example = SCHEMA_REMEMBER_ME_EXAMPLE, defaultValue = SCHEMA_REMEMBER_ME_DEFAULT)
    private Boolean rememberMe = false;

    // Constructors

    public LoginRequest() {
    }

    public LoginRequest(String usernameOrEmail, String password) {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }

    // Getters and Setters

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "usernameOrEmail='" + usernameOrEmail + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", rememberMe=" + rememberMe +
                '}';
    }
}
