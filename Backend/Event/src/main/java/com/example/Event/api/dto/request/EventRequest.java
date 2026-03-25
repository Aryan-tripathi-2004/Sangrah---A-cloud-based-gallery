package com.example.Event.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update an event")
public class EventRequest {

    @NotBlank(message = "Event name is required")
    @Schema(description = "Event name", example = "Birthday Party")
    private String eventName;

    @Schema(description = "Event description")
    private String eventDescription;

    @Schema(description = "Is event public", example = "true")
    private Boolean isPublic;

    @Schema(description = "Event category", example = "PERSONAL")
    private String eventCategory;

    @Schema(description = "Location of event")
    private String location;
}
