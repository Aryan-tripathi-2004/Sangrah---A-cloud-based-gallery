package com.example.Event.infrastructure.client;

import com.example.Event.infrastructure.client.dto.AuthUserLookupResponse;
import com.example.Event.shared.exception.DomainValidationException;
import com.example.Event.shared.exception.ResourceNotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Integration service for user identity lookups against Auth Service.
 * Decouples domain validation, exception translation, and fallback mapping
 * from the declarative Feign HTTP contract (adhering to SRP).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserIntegrationService {

    private final UserServiceClient userServiceClient;

    public String getUserIdByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        try {
            log.debug("Fetching user ID by email: {}", email);
            AuthUserLookupResponse response = userServiceClient.findUser(null, email);
            if (response != null) {
                String userId = response.resolvedUserId();
                if (userId != null && !userId.isBlank()) {
                    log.info("Resolved userId {} for email {}", userId, email);
                    return userId;
                }
            }
        } catch (FeignException e) {
            log.error("Network error fetching user ID for email {}: {}", email, e.getMessage(), e);
            throw new DomainValidationException("Unable to verify user identity against Auth Service.");
        } catch (Exception e) {
            log.error("Unexpected error fetching user ID for email {}: {}", email, e.getMessage(), e);
            throw new DomainValidationException("Unable to process user lookup.");
        }

        throw new ResourceNotFoundException("Could not find user with email: " + email);
    }

    public String getUserDisplayName(String userId) {
        if (userId == null || userId.isBlank()) {
            return "Unknown";
        }

        try {
            log.debug("Fetching user info from Auth Service for id: {}", userId);
            AuthUserLookupResponse response = userServiceClient.findUser(userId, null);
            if (response != null) {
                String displayName = response.resolvedDisplayName();
                if (displayName != null && !displayName.isBlank()) {
                    return displayName;
                }

                String email = response.resolvedEmail();
                if (email != null && !email.isBlank()) {
                    return email;
                }
            }
        } catch (FeignException e) {
            log.error("Network error fetching user display name for {}: {}", userId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching user display name for {}: {}", userId, e.getMessage(), e);
        }

        return userId;
    }

    public String getUserEmail(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("UserId cannot be null or empty");
        }

        try {
            log.debug("Fetching user info from Auth Service for id: {}", userId);
            AuthUserLookupResponse response = userServiceClient.findUser(userId, null);
            if (response != null) {
                String email = response.resolvedEmail();
                if (email != null && !email.isBlank()) {
                    return email;
                }
            }
        } catch (FeignException e) {
            log.error("Network error fetching user email for {}: {}", userId, e.getMessage(), e);
            throw new DomainValidationException("Unable to verify user identity against Auth Service.");
        } catch (Exception e) {
            log.error("Unexpected error fetching user email for {}: {}", userId, e.getMessage(), e);
            throw new DomainValidationException("Unable to process user lookup.");
        }

        throw new ResourceNotFoundException("Could not find email for userId: " + userId);
    }
}
