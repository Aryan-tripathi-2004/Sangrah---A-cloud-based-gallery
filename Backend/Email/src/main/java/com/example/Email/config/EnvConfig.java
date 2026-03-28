package com.example.Email.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Spring Configuration class for Email service
 * Verifies that SMTP configuration is loaded from .env file
 *
 * Spring Boot 3 automatically loads .env file via spring.config.import in application.properties
 */
@Slf4j
@Configuration
public class EnvConfig {

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    /**
     * Verify SMTP configuration is loaded
     */
    @PostConstruct
    public void verifySMTPConfig() {
        if (mailHost != null && !mailHost.isEmpty()) {
            log.info("✅ Email service configuration loaded");
            log.debug("📧 SMTP Host: {}", mailHost);
            log.debug("📧 SMTP User: {}", mailUsername);
        } else {
            log.warn("⚠️ WARNING: SMTP host not configured. Email service may not work properly.");
            log.warn("   Check .env file for MAIL_HOST variable");
        }
    }
}
