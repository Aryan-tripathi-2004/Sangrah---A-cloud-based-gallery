package com.example.Auth.dto;

import com.example.Auth.validation.ValidPassword;
import com.example.Auth.common.logging.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import static com.example.Auth.util.SchemaConstants.*;

/**
 * Request DTO for user registration.
 */
@Schema(description = SCHEMA_REGISTER_REQUEST_DESC)
public class RegisterRequest {

    @Schema(description = SCHEMA_USERNAME_DESC, example = SCHEMA_USERNAME_EXAMPLE, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, hyphens, and underscores")
    private String username;

    @Schema(description = SCHEMA_EMAIL_DESC, example = SCHEMA_EMAIL_EXAMPLE, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    @Sensitive
    private String email;

    @Schema(description = SCHEMA_PASSWORD_DESC, example = SCHEMA_PASSWORD_EXAMPLE, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @ValidPassword
    @Sensitive(maskChar = '*', showFirst = 0, showLast = 0)
    private String password;

    @Schema(description = SCHEMA_FIRST_NAME_DESC, example = SCHEMA_FIRST_NAME_EXAMPLE, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Schema(description = SCHEMA_LAST_NAME_DESC, example = SCHEMA_LAST_NAME_EXAMPLE, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Schema(description = SCHEMA_DISPLAY_NAME_DESC, example = SCHEMA_DISPLAY_NAME_EXAMPLE, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 150, message = "Display name cannot exceed 150 characters")
    private String displayName;

    @Schema(description = SCHEMA_MARKETING_OPT_IN_DESC, example = SCHEMA_MARKETING_OPT_IN_EXAMPLE, defaultValue = SCHEMA_MARKETING_OPT_IN_DEFAULT)
    private Boolean marketingOptIn = false;

    // Constructors

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Getters and Setters

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getMarketingOptIn() {
        return marketingOptIn;
    }

    public void setMarketingOptIn(Boolean marketingOptIn) {
        this.marketingOptIn = marketingOptIn;
    }

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", marketingOptIn=" + marketingOptIn +
                '}';
    }
}
