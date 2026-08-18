package com.example.Event.api.dto.response;

import java.util.List;

public record AccessRequestsResponse(
        String eventId,
        List<AccessRequestResponse> requests
) {
}
