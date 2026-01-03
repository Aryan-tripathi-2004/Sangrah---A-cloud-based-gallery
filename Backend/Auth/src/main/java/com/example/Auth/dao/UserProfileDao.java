package com.example.Auth.dao;

import com.example.Auth.entity.UserProfile;
import com.example.Auth.model.UserProfileModel;
import com.example.Auth.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO for UserProfile operations.
 */
@Component
public class UserProfileDao extends BaseDao<UserProfile, UserProfileModel, UUID> {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Override
    protected Class<UserProfile> getEntityClass() {
        return UserProfile.class;
    }

    @Override
    protected UserProfileModel toModel(UserProfile entity) {
        if (entity == null)
            return null;

        UserProfileModel model = new UserProfileModel();
        model.setId(entity.getId());
        model.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        model.setFirstName(entity.getFirstName());
        model.setLastName(entity.getLastName());
        model.setDisplayName(entity.getDisplayName());
        model.setBio(entity.getBio());
        model.setLocale(entity.getLocale());
        model.setTimezone(entity.getTimezone());
        model.setEmailVerified(entity.getEmailVerified());
        model.setProfilePublic(entity.getProfilePublic());
        model.setMarketingOptIn(entity.getMarketingOptIn());
        model.setTwoFactorEnabled(entity.getTwoFactorEnabled());
        model.setPremium(entity.getPremium());
        model.setDeleted(entity.getDeleted());
        model.setSuspended(entity.getSuspended());
        model.setTestAccount(entity.getTestAccount());
        model.setEmailNotificationsEnabled(entity.getEmailNotificationsEnabled());
        model.setPushNotificationsEnabled(entity.getPushNotificationsEnabled());
        model.setRulesAcceptanceVersion(entity.getRulesAcceptanceVersion());
        model.setRulesAcceptanceDate(entity.getRulesAcceptanceDate());
        model.setPrivacyPolicyAcceptanceVersion(entity.getPrivacyPolicyAcceptanceVersion());
        model.setPrivacyPolicyAcceptanceDate(entity.getPrivacyPolicyAcceptanceDate());
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());

        return model;
    }

    @Override
    protected UserProfile toEntity(UserProfileModel model) {
        if (model == null)
            return null;

        UserProfile entity = new UserProfile();
        entity.setId(model.getId());
        entity.setFirstName(model.getFirstName());
        entity.setLastName(model.getLastName());
        entity.setDisplayName(model.getDisplayName());
        entity.setBio(model.getBio());
        entity.setLocale(model.getLocale());
        entity.setTimezone(model.getTimezone());
        entity.setEmailVerified(model.getEmailVerified());
        entity.setProfilePublic(model.getProfilePublic());
        entity.setMarketingOptIn(model.getMarketingOptIn());
        entity.setTwoFactorEnabled(model.getTwoFactorEnabled());
        entity.setPremium(model.getPremium());
        entity.setDeleted(model.getDeleted());
        entity.setSuspended(model.getSuspended());
        entity.setTestAccount(model.getTestAccount());
        entity.setEmailNotificationsEnabled(model.getEmailNotificationsEnabled());
        entity.setPushNotificationsEnabled(model.getPushNotificationsEnabled());
        entity.setRulesAcceptanceVersion(model.getRulesAcceptanceVersion());
        entity.setRulesAcceptanceDate(model.getRulesAcceptanceDate());
        entity.setPrivacyPolicyAcceptanceVersion(model.getPrivacyPolicyAcceptanceVersion());
        entity.setPrivacyPolicyAcceptanceDate(model.getPrivacyPolicyAcceptanceDate());

        return entity;
    }

    // Query methods

    public Optional<UserProfileModel> findByUserId(UUID userId) {
        return userProfileRepository.findByUserId(userId).map(this::toModel);
    }

    public List<UserProfileModel> findByEmailVerifiedTrue() {
        return userProfileRepository.findByEmailVerifiedTrue().stream()
                .map(this::toModel).toList();
    }

    public List<UserProfileModel> findByPremiumTrue() {
        return userProfileRepository.findByPremiumTrue().stream()
                .map(this::toModel).toList();
    }

    public long countByPremiumTrue() {
        return userProfileRepository.countByPremiumTrue();
    }

    public long countByEmailVerifiedTrue() {
        return userProfileRepository.countByEmailVerifiedTrue();
    }

    // Update operations

    public boolean updateEmailVerified(UUID userId, boolean verified) {
        Query query = new Query(Criteria.where("user.$id").is(userId));
        Update update = new Update().set("emailVerified", verified);
        return updateOne(query, update);
    }

    public boolean updatePremiumStatus(UUID userId, boolean premium) {
        Query query = new Query(Criteria.where("user.$id").is(userId));
        Update update = new Update().set("premium", premium);
        return updateOne(query, update);
    }

    public boolean updateTwoFactorEnabled(UUID userId, boolean enabled) {
        Query query = new Query(Criteria.where("user.$id").is(userId));
        Update update = new Update().set("twoFactorEnabled", enabled);
        return updateOne(query, update);
    }

    public boolean updateSuspendedStatus(UUID userId, boolean suspended) {
        Query query = new Query(Criteria.where("user.$id").is(userId));
        Update update = new Update().set("suspended", suspended);
        return updateOne(query, update);
    }

    public boolean softDelete(UUID userId) {
        Query query = new Query(Criteria.where("user.$id").is(userId));
        Update update = new Update().set("deleted", true);
        return updateOne(query, update);
    }

    public boolean restore(UUID userId) {
        Query query = new Query(Criteria.where("user.$id").is(userId));
        Update update = new Update().set("deleted", false);
        return updateOne(query, update);
    }

    public boolean updateTermsAcceptance(UUID userId, String rulesVersion, String privacyVersion) {
        Query query = new Query(Criteria.where("user.$id").is(userId));
        Update update = new Update()
                .set("rulesAcceptanceVersion", rulesVersion)
                .set("rulesAcceptanceDate", Instant.now())
                .set("privacyPolicyAcceptanceVersion", privacyVersion)
                .set("privacyPolicyAcceptanceDate", Instant.now());
        return updateOne(query, update);
    }
}
