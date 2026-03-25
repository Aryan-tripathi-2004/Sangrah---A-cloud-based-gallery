package com.example.Billing.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class to load .env file
 * Loads environment variables from .env file into System properties
 */
@Slf4j
@Configuration
public class EnvConfig {

    @PostConstruct
    public void loadEnv() {
        try {
            // Load .env file from project root
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            log.info("✅ .env file loaded successfully");

            // Load all .env variables into System properties
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
                if (!entry.getKey().contains("PASSWORD") && !entry.getKey().contains("SECRET") && !entry.getKey().contains("KEY")) {
                    log.debug("  • {} = {}", entry.getKey(), entry.getValue());
                } else {
                    log.debug("  • {} = ****", entry.getKey());
                }
            });

            log.info("💚 Environment variables loaded: {} total", dotenv.entries().size());

        } catch (Exception e) {
            log.warn("⚠️ Could not load .env file: {}", e.getMessage());
            log.warn("   Application will use default values or system environment variables");
        }
    }
}
