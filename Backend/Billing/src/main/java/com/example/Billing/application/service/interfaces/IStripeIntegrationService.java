package com.example.Billing.application.service.interfaces;

import com.example.Billing.api.dto.response.StripeConfigResponse;
import com.example.Billing.api.dto.response.UserBillingSettingsDTO;

/**
 * Service contract for Stripe gateway integration settings and customer identities.
 * Manages customer preferences, billing addresses, and payment gateway configuration.
 */
public interface IStripeIntegrationService {

    /**
     * Retrieves existing user billing settings or initializes defaults.
     *
     * @param userId user identifier
     * @return user billing settings DTO
     */
    UserBillingSettingsDTO getUserSettings(String userId);

    /**
     * Creates and persists default billing settings for a user.
     *
     * @param userId user identifier
     * @return newly created user billing settings DTO
     */
    UserBillingSettingsDTO createDefaultUserSettings(String userId);

    /**
     * Retrieves the current Stripe gateway configuration status and publishable key.
     *
     * @return Stripe configuration response with connection status and public key
     */
    StripeConfigResponse getStripeConfig();
}
