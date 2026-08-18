package com.example.Gallery.api.dto.response;

import com.example.Gallery.shared.enums.TimelineGroupType;
import java.util.List;

public record TimelineGroupResponse(
    TimelineGroupType groupType,
    String label,
    int count,
    List<MediaItemResponse> items
) {}
