package com.example.Event.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventMediaModerationResponse(
        String status,
        String reason,
        String message
) {
}
