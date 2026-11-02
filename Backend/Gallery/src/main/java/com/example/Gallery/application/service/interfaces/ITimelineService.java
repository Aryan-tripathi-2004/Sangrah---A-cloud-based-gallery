package com.example.Gallery.application.service.interfaces;

import com.example.Gallery.api.dto.response.TimelineResponse;
import com.example.Gallery.shared.enums.TimelineGroupType;

public interface ITimelineService {
    TimelineResponse getTimeline(String userId, TimelineGroupType groupBy);
    TimelineResponse getDailyTimeline(String userId);
    TimelineResponse getMonthlyTimeline(String userId);
    TimelineResponse getYearlyTimeline(String userId);
}
