package com.example.Event.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;
    
    @Value("${auth.service.url:http://auth:8401}")
    private String authServiceUrl;

    /**
     * Fetch user ID by email
     */
    public String getUserIdByEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        try {
            String url = authServiceUrl + "/api/v1/users/by-email/" + email;
            log.debug("📡 Fetching user ID by email from: {}", url);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            log.debug("📡 Auth service response: {}", response);
            
            if (response != null) {
                // Check for userId directly in response
                if (response.containsKey("userId")) {
                    Object userId = response.get("userId");
                    if (userId != null && !userId.toString().isEmpty()) {
                        log.info("✅ Got userId: {} for email: {}", userId, email);
                        return userId.toString();
                    }
                }
                
                // Check for data object with id or userId
                if (response.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    if (data != null) {
                        if (data.containsKey("id") && data.get("id") != null) {
                            log.info("✅ Got id from data: {} for email: {}", data.get("id"), email);
                            return data.get("id").toString();
                        }
                        if (data.containsKey("userId") && data.get("userId") != null) {
                            log.info("✅ Got userId from data: {} for email: {}", data.get("userId"), email);
                            return data.get("userId").toString();
                        }
                    }
                }
            }
        } catch (RestClientException e) {
            log.warn("⚠️ Could not fetch user ID for email {}: {} (Auth service may be unavailable)", email, e.getMessage());
        } catch (Exception e) {
            log.warn("⚠️ Error fetching user ID for email {}: {}", email, e.getMessage(), e);
        }
        
        throw new RuntimeException("Could not find user with email: " + email);
    }

    /**
     * Fetch user display name by userId
     * Falls back to userId if user is not found
     */
    public String getUserDisplayName(String userId) {
        if (userId == null || userId.isEmpty()) {
            return "Unknown";
        }
        
        try {
            String url = authServiceUrl + "/api/v1/users/" + userId;
            log.debug("📡 Fetching user info from: {}", url);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            log.debug("📡 Auth service response: {}", response);
            
            if (response != null && response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.containsKey("displayName")) {
                    Object displayName = data.get("displayName");
                    if (displayName != null && !displayName.toString().isEmpty()) {
                        log.debug("✅ Got displayName: {} for userId: {}", displayName, userId);
                        return displayName.toString();
                    }
                }
                // Fallback to email if displayName is not available
                if (data != null && data.containsKey("email")) {
                    Object email = data.get("email");
                    if (email != null && !email.toString().isEmpty()) {
                        log.debug("✅ Got email as fallback: {} for userId: {}", email, userId);
                        return email.toString();
                    }
                }
            }
        } catch (RestClientException e) {
            log.warn("⚠️ Could not fetch user display name for {}: {} (Auth service may be unavailable)", userId, e.getMessage());
        } catch (Exception e) {
            log.warn("⚠️ Error fetching user display name for {}: {}", userId, e.getMessage(), e);
        }
        
        // Fallback to userId if user service is unavailable
        log.debug("⚠️ Returning fallback userId: {}", userId);
        return userId;
    }
}
