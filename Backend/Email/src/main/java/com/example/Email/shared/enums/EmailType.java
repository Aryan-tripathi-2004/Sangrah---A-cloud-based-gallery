package com.example.Email.shared.enums;

/**
 * Represents all supported email notification categories in the Sangrah platform.
 * Eliminates primitive String obsession on the emailType field of EmailLogDocument.
 */
public enum EmailType {

    /** Confirmation email sent after a successful invoice payment. */
    INVOICE_PAID,

    /** Notification email sent when a new invoice is generated. */
    INVOICE_CREATED,

    /** Alert email sent when a payment attempt fails. */
    PAYMENT_FAILED,

    /** Generic notification email (e.g. system alerts). */
    NOTIFICATION,

    /** Notification sent when a collaborator is added to a gallery. */
    COLLABORATOR_ADDED,

    /** Notification sent when a user requests access to a resource. */
    ACCESS_REQUEST,

    /** Notification sent when an access request is approved. */
    ACCESS_APPROVED,

    /** Notification sent when an access request is rejected. */
    ACCESS_REJECTED,

    /** Notification sent when a user's access is revoked. */
    ACCESS_REVOKED,

    /** Notification sent when media content is approved. */
    MEDIA_APPROVED,

    /** Notification sent when media content is rejected. */
    MEDIA_REJECTED
}
