package com.example.Auth.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Business model for UserProfile.
 * Domain model for user profile information.
 */
public class UserProfileModel {

    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String displayName;
    private String bio;
    private String locale;
    private String timezone;
    private Boolean emailVerified;
    private Boolean profilePublic;
    private Boolean marketingOptIn;
    private Boolean twoFactorEnabled;
    private Boolean premium;
    private Boolean deleted;
    private Boolean suspended;
    private Boolean testAccount;
    private Boolean emailNotificationsEnabled;
    private Boolean pushNotificationsEnabled;
    private String rulesAcceptanceVersion;
    private Instant rulesAcceptanceDate;
    private String privacyPolicyAcceptanceVersion;
    private Instant privacyPolicyAcceptanceDate;
    private Instant createdAt;
    private Instant updatedAt;

    // Constructors
    public UserProfileModel() {
    }

    public UserProfileModel(UUID userId) {
        this.userId = userId;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Boolean getProfilePublic() {
        return profilePublic;
    }

    public void setProfilePublic(Boolean profilePublic) {
        this.profilePublic = profilePublic;
    }

    public Boolean getMarketingOptIn() {
        return marketingOptIn;
    }

    public void setMarketingOptIn(Boolean marketingOptIn) {
        this.marketingOptIn = marketingOptIn;
    }

    public Boolean getTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public Boolean getPremium() {
        return premium;
    }

    public void setPremium(Boolean premium) {
        this.premium = premium;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Boolean getSuspended() {
        return suspended;
    }

    public void setSuspended(Boolean suspended) {
        this.suspended = suspended;
    }

    public Boolean getTestAccount() {
        return testAccount;
    }

    public void setTestAccount(Boolean testAccount) {
        this.testAccount = testAccount;
    }

    public Boolean getEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(Boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public Boolean getPushNotificationsEnabled() {
        return pushNotificationsEnabled;
    }

    public void setPushNotificationsEnabled(Boolean pushNotificationsEnabled) {
        this.pushNotificationsEnabled = pushNotificationsEnabled;
    }

    public String getRulesAcceptanceVersion() {
        return rulesAcceptanceVersion;
    }

    public void setRulesAcceptanceVersion(String rulesAcceptanceVersion) {
        this.rulesAcceptanceVersion = rulesAcceptanceVersion;
    }

    public Instant getRulesAcceptanceDate() {
        return rulesAcceptanceDate;
    }

    public void setRulesAcceptanceDate(Instant rulesAcceptanceDate) {
        this.rulesAcceptanceDate = rulesAcceptanceDate;
    }

    public String getPrivacyPolicyAcceptanceVersion() {
        return privacyPolicyAcceptanceVersion;
    }

    public void setPrivacyPolicyAcceptanceVersion(String privacyPolicyAcceptanceVersion) {
        this.privacyPolicyAcceptanceVersion = privacyPolicyAcceptanceVersion;
    }

    public Instant getPrivacyPolicyAcceptanceDate() {
        return privacyPolicyAcceptanceDate;
    }

    public void setPrivacyPolicyAcceptanceDate(Instant privacyPolicyAcceptanceDate) {
        this.privacyPolicyAcceptanceDate = privacyPolicyAcceptanceDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Business logic methods
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return displayName != null ? displayName : "Unknown";
    }

    public boolean hasAcceptedLatestTerms(String currentRulesVersion, String currentPrivacyVersion) {
        return currentRulesVersion.equals(rulesAcceptanceVersion) &&
                currentPrivacyVersion.equals(privacyPolicyAcceptanceVersion);
    }

    @Override
    public String toString() {
        return "UserProfileModel{" +
                "id=" + id +
                ", userId=" + userId +
                ", displayName='" + displayName + '\'' +
                ", emailVerified=" + emailVerified +
                ", premium=" + premium +
                '}';
    }
}
