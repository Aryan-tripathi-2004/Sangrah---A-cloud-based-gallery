package com.example.Gallery.api.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record TimelineResponse(
    String view,
    List<TimelineGroupResponse> groups,
    int totalGroups,
    int page,
    int size,
    boolean hasMore
) {
    public TimelineResponse(String view, List<TimelineGroupResponse> groups, int totalGroups) {
        this(view, groups, totalGroups, 0, totalGroups, false);
    }
}
