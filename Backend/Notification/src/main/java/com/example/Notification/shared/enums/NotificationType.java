package com.example.Notification.shared.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the type of a notification across the Sangrah platform.
 */
public enum NotificationType {
    INFO,
    WARNING,
    ERROR,
    SUCCESS,
    COLLABORATOR_ADDED,
    COLLABORATOR_REMOVED,
    ACCESS_REQUEST,
    ACCESS_REQUEST_RECEIVED,
    ACCESS_APPROVED,
    ACCESS_REQUEST_APPROVED,
    ACCESS_REJECTED,
    ACCESS_REQUEST_REJECTED,
    ACCESS_REVOKED,
    MEDIA_APPROVED,
    MEDIA_REJECTED,
    EVENT_UPDATED,
    INVOICE_PAID,
    INVOICE_CREATED,
    PAYMENT_FAILED,
    NOTIFICATION;

    @JsonCreator
    public static NotificationType fromString(String value) {
        if (value == null || value.isBlank()) {
            return INFO;
        }
        String normalized = value.trim();
        for (NotificationType type : values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return INFO;
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
