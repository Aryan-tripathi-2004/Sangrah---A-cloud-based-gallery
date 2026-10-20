package com.example.Gallery.api.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record TimelineResponse(
    String view,
    List<TimelineGroupResponse> groups,
    int totalGroups
) {}
