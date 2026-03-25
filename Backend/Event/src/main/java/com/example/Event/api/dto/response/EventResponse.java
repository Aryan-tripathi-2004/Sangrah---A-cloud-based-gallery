package com.example.Event.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for event response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Event details")
public class EventResponse {

    @Schema(description = "Event ID", example = "507f1f77bcf86cd799439011")
    private String eventId;

    @Schema(description = "Event name", example = "Birthday Party")
    private String eventName;

    @Schema(description = "Event description")
    private String eventDescription;

    @Schema(description = "Creator user ID")
    private String creatorId;

    @Schema(description = "Is event public")
    private Boolean isPublic;

    @Schema(description = "Event category", example = "PERSONAL")
    private String eventCategory;

    @Schema(description = "Location of event")
    private String location;

    @Schema(description = "List of participants (user IDs)")
    private List<String> participants;

    @Schema(description = "Event creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Event last update timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
