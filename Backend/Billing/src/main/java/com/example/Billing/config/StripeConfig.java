package com.example.Billing.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    /**
     * Initialises the Stripe SDK with the configured API key.
     * The key is read from the {@code stripe.api-key} property so that it is
     * never hard-coded in source code.  Set the environment variable
     * {@code STRIPE_API_KEY} (or override the property in your local
     * application.properties) to supply the value at runtime.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }
}
