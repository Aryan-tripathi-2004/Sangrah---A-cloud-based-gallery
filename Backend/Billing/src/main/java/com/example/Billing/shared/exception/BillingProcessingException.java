package com.example.Billing.shared.exception;

public class BillingProcessingException extends RuntimeException {
    public BillingProcessingException(String message) {
        super(message);
    }

    public BillingProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
