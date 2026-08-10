package com.example.Event.infrastructure.persistence.document;

import com.example.Event.shared.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
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
    @Version
    private Long version;

    @Indexed
    private String eventId;

    @Indexed
    private String mediaId;

    @Indexed
    private String uploaderUserId;

    private ApprovalStatus status;

    private String reviewedByUserId;  // Who approved/rejected this media
    private Instant reviewedAt;

    private String rejectionReason;  // If rejected, why?

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Check if approval is pending review
     */
    public boolean isPending() {
        return ApprovalStatus.PENDING == status;
    }

    /**
     * Check if approval is approved
     */
    public boolean isApproved() {
        return ApprovalStatus.APPROVED == status;
    }

    /**
     * Check if approval is rejected
     */
    public boolean isRejected() {
        return ApprovalStatus.REJECTED == status;
    }
}
