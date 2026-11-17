package com.example.Gallery.application.service.interfaces;

import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.TimelineResponse;
import com.example.Gallery.shared.enums.TimelineGroupType;
import org.springframework.data.domain.Page;

public interface ITimelineService {
    TimelineResponse getTimeline(String userId, TimelineGroupType groupBy);
    TimelineResponse getTimeline(String userId, TimelineGroupType groupBy, int page, int size);
    TimelineResponse getDailyTimeline(String userId);
    TimelineResponse getDailyTimeline(String userId, int page, int size);
    TimelineResponse getMonthlyTimeline(String userId);
    TimelineResponse getMonthlyTimeline(String userId, int page, int size);
    TimelineResponse getYearlyTimeline(String userId);
    TimelineResponse getYearlyTimeline(String userId, int page, int size);
    Page<MediaItemResponse> getGroupItems(String userId, TimelineGroupType groupBy, String groupDate, int page, int size);
}
