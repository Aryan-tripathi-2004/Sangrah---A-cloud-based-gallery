package com.example.Event.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Multipart event update request")
public class EventUpdateRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotBlank(message = "Event date is required")
    private String eventDate;

    @NotBlank(message = "Visibility is required")
    @Pattern(regexp = "PUBLIC|PROTECTED|PRIVATE", message = "Visibility must be PUBLIC, PROTECTED, or PRIVATE")
    private String visibility;

    private Boolean moderationEnabled;
}