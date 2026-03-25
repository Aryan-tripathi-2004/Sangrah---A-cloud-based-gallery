package com.example.Billing.infrastructure.persistence.repository;

import com.example.Billing.infrastructure.persistence.document.UserBillingSettingsDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserBillingSettingsRepository extends MongoRepository<UserBillingSettingsDocument, String> {

    Optional<UserBillingSettingsDocument> findByUserId(String userId);

    Optional<UserBillingSettingsDocument> findByStripeCustomerId(String stripeCustomerId);
}
