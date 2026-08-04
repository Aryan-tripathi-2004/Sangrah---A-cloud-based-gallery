package com.example.Event.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "event_media_approvals")
public class EventMediaApprovalDocument {

    @Id
    private String id;

    @Indexed
    private String eventId;

    @Indexed
    private String mediaId;

    @Indexed
    private String uploaderUserId;

    private String status;  // PENDING, APPROVED, REJECTED

    private String reviewedByUserId;  // Who approved/rejected this media
    private Instant reviewedAt;

    private String rejectionReason;  // If rejected, why?

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Check if approval is pending review
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    /**
     * Check if approval is approved
     */
    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    /**
     * Check if approval is rejected
     */
    public boolean isRejected() {
        return "REJECTED".equals(status);
    }
}
