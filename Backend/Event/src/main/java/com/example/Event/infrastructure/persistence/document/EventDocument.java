package com.example.Event.infrastructure.persistence.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("events")
public class EventDocument {
    @Id
    private String id;
    private String ownerUserId;
    private String title;
    private String description;
    private Instant eventDate;
    private String visibility;
    private boolean moderationEnabled;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    // NEW: Collaborator management
    private List<EventCollaborator> collaborators;

    // NEW: Upload & moderation policies



    // Inner class for collaborator with granular permissions
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventCollaborator {
        private String userId;                      // User's ID
        private Boolean canUploadMedia;             // Can upload media (subject to moderation)
        private Boolean canReviewMedia;             // Can approve/reject pending media
        private Boolean canReviewAccessRequests;    // Can approve/reject access requests
        private Boolean canDirectUpload;            // NEW: Can bypass moderation (direct upload)
        private Boolean canDeleteMedia;             // NEW: Can delete media from event
        private Boolean canEditEventDetails;        // NEW: Can edit event details
        private Instant addedAt;
        private String addedByUserId;               // Who added this collaborator (owner)
    }
}
