package com.example.Event.api.dto.response;

import java.util.List;

public record EventMediaCollectionResponse(
        String eventId,
        List<EventMediaItemResponse> media,
        String message,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasMore,
        long pendingCount,
        long approvedCount,
        long rejectedCount
) {
    public EventMediaCollectionResponse(String eventId, List<EventMediaItemResponse> media, String message) {
        this(eventId, media, message, 0, media != null ? media.size() : 0, media != null ? media.size() : 0L, 1, false, 0, 0, 0);
    }

    public EventMediaCollectionResponse(
            String eventId,
            List<EventMediaItemResponse> media,
            String message,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasMore) {
        this(eventId, media, message, page, size, totalElements, totalPages, hasMore, 0, 0, 0);
    }
}
