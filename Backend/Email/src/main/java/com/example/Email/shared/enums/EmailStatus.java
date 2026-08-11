package com.example.Email.shared.enums;

/**
 * Represents the lifecycle status of an email delivery attempt.
 * Eliminates primitive String obsession on the status field of EmailLogDocument.
 */
public enum EmailStatus {

    /** Email was successfully dispatched via SMTP. */
    SENT,

    /** Email has been queued but not yet dispatched. */
    PENDING,

    /** Email dispatch failed after one or more attempts. */
    FAILED,

    /** Email was rejected / bounced by the recipient's mail server. */
    BOUNCED
}
