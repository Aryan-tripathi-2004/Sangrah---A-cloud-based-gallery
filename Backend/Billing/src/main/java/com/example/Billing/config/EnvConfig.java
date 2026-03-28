package com.example.Billing.config;

import com.stripe.Stripe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Spring Configuration class to initialize critical services
 * Ensures Stripe API key is set after Spring loads properties from .env file
 *
 * Spring Boot 3 automatically loads .env file via spring.config.import in application.properties
 * This class just initializes the Stripe SDK with the loaded configuration
 */
@Slf4j
@Configuration
public class EnvConfig {

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    /**
     * Initialize Stripe API key after Spring properties are loaded
     * This runs AFTER Spring loads all properties from .env file
     */
    @PostConstruct
    public void initializeStripe() {
        if (stripeApiKey != null && !stripeApiKey.isEmpty()) {
            Stripe.apiKey = stripeApiKey;
            log.info("✅ Stripe API key initialized from properties");
            log.debug("🔑 Stripe key loaded: {}...", stripeApiKey.substring(0, 10));
        } else {
            log.error("❌ CRITICAL: Stripe API key is empty! Check .env file or STRIPE_API_KEY property");
            log.error("   Expected property: stripe.api.key=${STRIPE_API_KEY}");
            throw new IllegalStateException("Stripe API key not configured. Cannot initialize Billing service.");
        }
    }
}
