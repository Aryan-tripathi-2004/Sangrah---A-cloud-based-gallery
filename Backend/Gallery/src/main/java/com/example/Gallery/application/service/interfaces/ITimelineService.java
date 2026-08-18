package com.example.Gallery.application.service.interfaces;

import com.example.Gallery.api.dto.response.TimelineGroupResponse;

import java.util.List;

public interface ITimelineService {
    List<TimelineGroupResponse> getDailyTimeline(String userId);
    List<TimelineGroupResponse> getMonthlyTimeline(String userId);
    List<TimelineGroupResponse> getYearlyTimeline(String userId);
}
