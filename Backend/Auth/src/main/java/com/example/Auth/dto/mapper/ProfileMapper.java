package com.example.Auth.dto.mapper;

import com.example.Auth.dto.response.ProfileResponse;
import com.example.Auth.model.UserProfileModel;
import org.springframework.stereotype.Component;

/**
 * Mapper for UserProfile-related DTOs.
 */
@Component
public class ProfileMapper {

    /**
     * Convert UserProfileModel to ProfileResponse.
     */
    public ProfileResponse toResponse(UserProfileModel model) {
        if (model == null) {
            return null;
        }

        ProfileResponse response = new ProfileResponse();
        response.setId(model.getId());
        response.setUserId(model.getUserId());
        response.setFirstName(model.getFirstName());
        response.setLastName(model.getLastName());
        response.setDisplayName(model.getDisplayName());
        response.setBio(model.getBio());
        response.setLocale(model.getLocale());
        response.setTimezone(model.getTimezone());
        response.setEmailVerified(model.getEmailVerified());
        response.setProfilePublic(model.getProfilePublic());
        response.setMarketingOptIn(model.getMarketingOptIn());
        response.setTwoFactorEnabled(model.getTwoFactorEnabled());
        response.setPremium(model.getPremium());
        response.setSuspended(model.getSuspended());
        response.setTestAccount(model.getTestAccount());
        response.setEmailNotificationsEnabled(model.getEmailNotificationsEnabled());
        response.setPushNotificationsEnabled(model.getPushNotificationsEnabled());
        response.setRulesAcceptanceVersion(model.getRulesAcceptanceVersion());
        response.setRulesAcceptanceDate(model.getRulesAcceptanceDate());
        response.setPrivacyPolicyAcceptanceVersion(model.getPrivacyPolicyAcceptanceVersion());
        response.setPrivacyPolicyAcceptanceDate(model.getPrivacyPolicyAcceptanceDate());
        response.setCreatedAt(model.getCreatedAt());
        response.setUpdatedAt(model.getUpdatedAt());

        return response;
    }
}
