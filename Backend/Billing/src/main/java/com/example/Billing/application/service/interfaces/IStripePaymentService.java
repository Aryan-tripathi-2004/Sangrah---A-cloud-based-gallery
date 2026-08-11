package com.example.Billing.application.service.interfaces;

import com.stripe.exception.StripeException;

public interface IStripePaymentService {
    String createPaymentIntent(String userId, String invoiceId, Double amount) throws StripeException;
    void handlePaymentSuccess(String paymentIntentId);
    void handlePaymentFailed(String paymentIntentId, String failureReason);
    void retryFailedPayments();
    void syncPaymentStatusWithStripe(String invoiceId, String paymentIntentId);
}
