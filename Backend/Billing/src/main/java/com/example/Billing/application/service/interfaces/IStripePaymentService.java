package com.example.Billing.application.service.interfaces;

import java.math.BigDecimal;

import com.example.Billing.api.dto.response.CheckoutSessionResponse;
import com.example.Billing.api.dto.response.PaymentStatusResponse;
import com.stripe.exception.StripeException;

public interface IStripePaymentService {
    String createPaymentIntent(String userId, String invoiceId, BigDecimal amount) throws StripeException;
    void handlePaymentSuccess(String paymentIntentId);
    void handlePaymentFailed(String paymentIntentId, String failureReason);
    void retryFailedPayments();
    void syncPaymentStatusWithStripe(String invoiceId, String paymentIntentId);
    PaymentStatusResponse syncPaymentStatus(String invoiceId, String userId);

    /**
     * Creates a Stripe payment intent for an invoice and returns a checkout session response.
     * Internally fetches the invoice to get the amount, then creates the payment intent.
     */
    CheckoutSessionResponse createCheckoutSession(String userId, String invoiceId) throws StripeException;

    /**
     * Returns the payment status for a given invoice.
     */
    PaymentStatusResponse getPaymentStatus(String userId, String invoiceId);
}
