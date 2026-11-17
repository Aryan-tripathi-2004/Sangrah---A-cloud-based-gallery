package com.example.Gallery.api.dto.response;

import com.example.Gallery.shared.enums.TimelineGroupType;
import lombok.Builder;

import java.util.List;

@Builder
public record TimelineGroupResponse(
    String type,
    TimelineGroupType groupType,
    String label,
    String date,
    int count,
    List<MediaItemResponse> items,
    int page,
    int size,
    long totalElements,
    boolean hasMore
) {
    public TimelineGroupResponse(
        String type,
        TimelineGroupType groupType,
        String label,
        int count,
        List<MediaItemResponse> items
    ) {
        this(type, groupType, label, label, count, items, 0, count, count, false);
    }

    public TimelineGroupResponse(
        String type,
        TimelineGroupType groupType,
        String label,
        int count,
        List<MediaItemResponse> items,
        int page,
        int size,
        long totalElements,
        boolean hasMore
    ) {
        this(type, groupType, label, label, count, items, page, size, totalElements, hasMore);
    }
}
