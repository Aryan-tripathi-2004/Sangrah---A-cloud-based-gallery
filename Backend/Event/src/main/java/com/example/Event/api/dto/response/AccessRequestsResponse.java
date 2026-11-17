package com.example.Event.api.dto.response;

import java.util.List;

public record AccessRequestsResponse(
        String eventId,
        List<AccessRequestResponse> requests,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasMore
) {
    public AccessRequestsResponse(String eventId, List<AccessRequestResponse> requests) {
        this(eventId, requests, 0, requests != null ? requests.size() : 0, requests != null ? (long) requests.size() : 0L, 1, false);
    }
}
