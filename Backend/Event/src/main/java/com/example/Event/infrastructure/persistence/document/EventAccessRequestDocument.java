package com.example.Event.infrastructure.persistence.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("event_access_requests")
public class EventAccessRequestDocument {
    @Id
    private String id;
    private String eventId;
    private String requesterUserId;
    private String status;                          // PENDING | APPROVED | REJECTED
    private Instant requestedAt;
    private Instant decisionAt;
    private String decidedByUserId;

    // NEW: Message from requester explaining WHY they want access
    private String message;                         // Optional message from requester

    // NEW: Rejection details
    private String rejectionReason;                 // If REJECTED, why

    // NEW: Access duration settings
    private String approvalDuration;                // "FOREVER" | "UNTIL_DATE"
    private Instant accessExpiresAt;                // When access expires (if UNTIL_DATE); null if FOREVER

    // NEW: Notification tracking
    private Boolean emailNotificationSent;          // Email sent to requester on approval/rejection
    private Boolean inAppNotificationCreated;       // In-app notification created for requester

    // NEW: Revocation tracking (for audit trail + enabling re-requests)
    private Instant revokedAt;                      // When owner revoked access (null if not revoked)
    private String revokedByUserId;                 // Who revoked access - owner ID (null if not revoked)

    // NEW: Track when record was last updated (for audit trail)
    private Instant updatedAt;                      // When record was last updated
}
