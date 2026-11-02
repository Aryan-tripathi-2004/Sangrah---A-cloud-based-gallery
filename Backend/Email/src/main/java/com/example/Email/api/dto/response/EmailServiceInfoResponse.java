package com.example.Email.api.dto.response;

import com.example.Email.shared.enums.EmailType;

/**
 * Immutable response record describing the capabilities of this Email microservice.
 *
 * <p>Extracted from the static inner class {@code EmailSendController.EmailServiceInfo}
 * to honour the Single Responsibility Principle and eliminate the inner-class
 * anti-pattern from the controller.
 *
 * <p>The {@code supportedEmailTypes} array is derived from the canonical
 * {@link EmailType} enum values at construction time,
 * so it always stays in sync with the enum without manual maintenance.
 *
 * <ul>
 *   <li>{@code serviceName}        – display name of the microservice.</li>
 *   <li>{@code version}            – semantic version string.</li>
 *   <li>{@code status}             – operational status (e.g. "active").</li>
 *   <li>{@code supportedEmailTypes}– all enum constant names from {@link EmailType}.</li>
 * </ul>
 */
public record EmailServiceInfoResponse(

        String serviceName,

        String version,

        String status,

        String[] supportedEmailTypes
) {

    /**
     * Factory method that builds a pre-populated info response with the
     * complete list of {@link EmailType} values.
     */
    public static EmailServiceInfoResponse defaults() {
        EmailType[] types = EmailType.values();
        String[] typeNames = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typeNames[i] = types[i].name();
        }
        return new EmailServiceInfoResponse(
                "Email Notification Service",
                "2.0.0",
                "active",
                typeNames
        );
    }
}
