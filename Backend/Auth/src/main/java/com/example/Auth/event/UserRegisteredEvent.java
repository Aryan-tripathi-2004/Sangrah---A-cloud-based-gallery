package com.example.Auth.event;

import com.example.Auth.common.event.BaseEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event published when a new user successfully registers.
 * This event triggers async operations like:
 * - Sending verification email
 * - Creating default storage space
 * - Initializing user preferences
 */
public class UserRegisteredEvent extends BaseEvent {

    private static final String EVENT_TYPE = "user.registered";

    private final String userId;
    private final String username;
    private final String email;
    private final String displayName;
    private final boolean requiresEmailVerification;
    private final String verificationToken;

    public UserRegisteredEvent(String userId, String username, String email,
            String displayName, boolean requiresEmailVerification,
            String verificationToken) {
        super(EVENT_TYPE, userId);
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.requiresEmailVerification = requiresEmailVerification;
        this.verificationToken = verificationToken;
    }

    /**
     * Returns the event payload as a map for serialization.
     */
    public Map<String, Object> getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("email", email);
        payload.put("displayName", displayName);
        payload.put("requiresEmailVerification", requiresEmailVerification);
        payload.put("verificationToken", verificationToken);
        payload.put("timestamp", Instant.now().toString());
        return payload;
    }

    // Getters

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isRequiresEmailVerification() {
        return requiresEmailVerification;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    @Override
    public String toString() {
        return "UserRegisteredEvent{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", requiresEmailVerification=" + requiresEmailVerification +
                ", eventId='" + getEventId() + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
