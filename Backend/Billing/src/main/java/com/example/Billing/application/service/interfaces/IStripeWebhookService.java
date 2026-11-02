package com.example.Billing.application.service.interfaces;

import com.example.Billing.api.dto.response.WebhookResponse;
import com.stripe.exception.SignatureVerificationException;

public interface IStripeWebhookService {
    WebhookResponse processWebhook(String payload, String signature) throws SignatureVerificationException;
}
