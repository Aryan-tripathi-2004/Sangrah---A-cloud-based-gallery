package com.example.Billing.application.service.impl;

import com.example.Billing.api.dto.response.StripeConfigResponse;
import com.example.Billing.api.dto.response.UserBillingSettingsDTO;
import com.example.Billing.application.service.interfaces.IStripeIntegrationService;
import com.example.Billing.infrastructure.mapper.UserBillingSettingsMapper;
import com.example.Billing.infrastructure.persistence.document.UserBillingSettingsDocument;
import com.example.Billing.infrastructure.persistence.repository.UserBillingSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Implementation of {@link IStripeIntegrationService}.
 * Manages customer billing settings, default preferences, and Stripe configuration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeIntegrationServiceImpl implements IStripeIntegrationService {

    private final UserBillingSettingsRepository userSettingsRepository;
    private final UserBillingSettingsMapper userBillingSettingsMapper;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    @Value("${billing.default.cost-alert-threshold}")
    private double defaultCostAlertThreshold;

    @Override
    public UserBillingSettingsDTO getUserSettings(String userId) {
        UserBillingSettingsDocument doc = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettingsDoc(userId));
        return userBillingSettingsMapper.toDTO(doc);
    }

    @Override
    public UserBillingSettingsDTO createDefaultUserSettings(String userId) {
        UserBillingSettingsDocument doc = createDefaultSettingsDoc(userId);
        return userBillingSettingsMapper.toDTO(doc);
    }

    private UserBillingSettingsDocument createDefaultSettingsDoc(String userId) {
        log.info("📝 Creating default billing settings for user {}", userId);

        UserBillingSettingsDocument settings = UserBillingSettingsDocument.builder()
                .userId(userId)
                .stripeCustomerId(null)  // Will be set when user adds payment method
                .paymentMethods(null)
                .totalPaidAllTime(BigDecimal.ZERO)
                .outstandingBalance(BigDecimal.ZERO)
                .billing(UserBillingSettingsDocument.BillingAddress.builder()
                        .email(null)
                        .address(null)
                        .city(null)
                        .postalCode(null)
                        .country(null)
                        .taxId(null)
                        .build())
                .preferences(UserBillingSettingsDocument.BillingPreferences.builder()
                        .autoPayEnabled(true)
                        .invoiceEmail(true)
                        .costAlertThreshold(BigDecimal.valueOf(defaultCostAlertThreshold))
                        .build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return userSettingsRepository.save(settings);
    }

    @Override
    public StripeConfigResponse getStripeConfig() {
        boolean stripeConfigured = stripePublishableKey != null && !stripePublishableKey.isEmpty();
        return StripeConfigResponse.builder()
                .stripePublishableKey(stripeConfigured ? stripePublishableKey : null)
                .stripeConfigured(stripeConfigured)
                .message(stripeConfigured ? "Stripe is configured and ready for payments"
                        : "Stripe is not configured. Please contact support.")
                .build();
    }
}
