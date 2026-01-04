package com.example.Auth.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * User profile entity containing extended user information and preferences.
 */
@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_profile_user", columnList = "user_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class UserProfile {

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "locale", length = 10)
    private String locale = "en_US";

    @Column(name = "timezone", length = 50)
    private String timezone = "UTC";

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "profile_public", nullable = false)
    private Boolean profilePublic = false;

    @Column(name = "marketing_opt_in", nullable = false)
    private Boolean marketingOptIn = false;

    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled = false;

    @Column(name = "premium", nullable = false)
    private Boolean premium = false;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "suspended", nullable = false)
    private Boolean suspended = false;

    @Column(name = "test_account", nullable = false)
    private Boolean testAccount = false;

    @Column(name = "email_notifications_enabled", nullable = false)
    private Boolean emailNotificationsEnabled = true;

    @Column(name = "push_notifications_enabled", nullable = false)
    private Boolean pushNotificationsEnabled = true;

    @Column(name = "rules_acceptance_version", length = 20)
    private String rulesAcceptanceVersion;

    @Column(name = "rules_acceptance_date")
    private Instant rulesAcceptanceDate;

    @Column(name = "privacy_policy_acceptance_version", length = 20)
    private String privacyPolicyAcceptanceVersion;

    @Column(name = "privacy_policy_acceptance_date")
    private Instant privacyPolicyAcceptanceDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Constructors
    public UserProfile() {
        this.id = UUID.randomUUID();
    }

    public UserProfile(User user) {
        this();
        this.user = user;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
}
