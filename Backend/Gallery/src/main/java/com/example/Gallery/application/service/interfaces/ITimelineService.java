package com.example.Gallery.application.service.interfaces;

import com.example.Gallery.api.dto.response.TimelineResponse;

public interface ITimelineService {
    TimelineResponse getDailyTimeline(String userId);
    TimelineResponse getMonthlyTimeline(String userId);
    TimelineResponse getYearlyTimeline(String userId);
}
