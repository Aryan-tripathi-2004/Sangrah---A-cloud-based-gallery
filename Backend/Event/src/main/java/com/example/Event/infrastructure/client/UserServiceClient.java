package com.example.Event.infrastructure.client;

import com.example.Event.infrastructure.client.dto.AuthUserLookupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${auth.service.url:http://auth:8401}")
    private String authServiceUrl;

    public String getUserIdByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        try {
            String url = authServiceUrl + "/api/v1/users/by-email/" + email;
            log.debug("Fetching user ID by email from: {}", url);

            AuthUserLookupResponse response = restTemplate.getForObject(url, AuthUserLookupResponse.class);
            if (response != null) {
                String userId = response.resolvedUserId();
                if (userId != null && !userId.isBlank()) {
                    log.info("Resolved userId {} for email {}", userId, email);
                    return userId;
                }
            }
        } catch (RestClientException e) {
            log.warn("Could not fetch user ID for email {}: {}", email, e.getMessage());
        } catch (Exception e) {
            log.warn("Error fetching user ID for email {}: {}", email, e.getMessage(), e);
        }

        throw new RuntimeException("Could not find user with email: " + email);
    }

    public String getUserDisplayName(String userId) {
        if (userId == null || userId.isBlank()) {
            return "Unknown";
        }

        try {
            String url = authServiceUrl + "/api/v1/users/" + userId;
            log.debug("Fetching user info from: {}", url);

            AuthUserLookupResponse response = restTemplate.getForObject(url, AuthUserLookupResponse.class);
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
        } catch (RestClientException e) {
            log.warn("Could not fetch user display name for {}: {}", userId, e.getMessage());
        } catch (Exception e) {
            log.warn("Error fetching user display name for {}: {}", userId, e.getMessage(), e);
        }

        return userId;
    }

    public String getUserEmail(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("UserId cannot be null or empty");
        }

        try {
            String url = authServiceUrl + "/api/v1/users/" + userId;
            log.debug("Fetching user info from: {}", url);

            AuthUserLookupResponse response = restTemplate.getForObject(url, AuthUserLookupResponse.class);
            if (response != null) {
                String email = response.resolvedEmail();
                if (email != null && !email.isBlank()) {
                    return email;
                }
            }
        } catch (RestClientException e) {
            log.warn("Could not fetch user email for {}: {}", userId, e.getMessage());
        } catch (Exception e) {
            log.warn("Error fetching user email for {}: {}", userId, e.getMessage(), e);
        }

        throw new RuntimeException("Could not find email for userId: " + userId);
    }
}
