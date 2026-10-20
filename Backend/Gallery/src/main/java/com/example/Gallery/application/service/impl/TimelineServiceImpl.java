package com.example.Gallery.application.service.impl;

import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.TimelineGroupResponse;
import com.example.Gallery.api.dto.response.TimelineResponse;
import com.example.Gallery.application.service.interfaces.ITimelineService;
import com.example.Gallery.infrastructure.mapper.GalleryMediaMapper;
import com.example.Gallery.infrastructure.mapper.TimelineMapper;
import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
import com.example.Gallery.infrastructure.persistence.repository.GalleryMediaRepository;
import com.example.Gallery.infrastructure.persistence.repository.TimelineGroupAggregationResult;
import com.example.Gallery.shared.enums.TimelineGroupType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineServiceImpl implements ITimelineService {

    private final GalleryMediaRepository mediaRepository;
    private final GalleryMediaMapper galleryMediaMapper;
    private final TimelineMapper timelineMapper;

    @Override
    public TimelineResponse getDailyTimeline(String userId) {
        log.info("📅 Generating daily timeline for user: {}", userId);

        List<TimelineGroupAggregationResult> groupedData = mediaRepository.getGroupedTimelineDaily(userId);
        log.info("Found {} daily timeline groups for user: {}", groupedData.size(), userId);

        List<TimelineGroupResponse> groups = groupedData.stream()
                .map(group -> buildTimelineGroup(TimelineGroupType.DAILY, group.getDate(), group.getItems()))
                .toList();

        return new TimelineResponse("daily", groups, groups.size());
    }

    @Override
    public TimelineResponse getMonthlyTimeline(String userId) {
        log.info("📅 Generating monthly timeline for user: {}", userId);

        List<TimelineGroupAggregationResult> groupedData = mediaRepository.getGroupedTimelineMonthly(userId);
        log.info("Found {} monthly timeline groups for user: {}", groupedData.size(), userId);

        List<TimelineGroupResponse> groups = groupedData.stream()
                .map(group -> {
                    String label = formatMonthlyLabel(group.getDate());
                    return buildTimelineGroup(TimelineGroupType.MONTHLY, label, group.getItems());
                })
                .toList();

        return new TimelineResponse("monthly", groups, groups.size());
    }

    @Override
    public TimelineResponse getYearlyTimeline(String userId) {
        log.info("📅 Generating yearly timeline for user: {}", userId);

        List<TimelineGroupAggregationResult> groupedData = mediaRepository.getGroupedTimelineYearly(userId);
        log.info("Found {} yearly timeline groups for user: {}", groupedData.size(), userId);

        List<TimelineGroupResponse> groups = groupedData.stream()
                .map(group -> buildTimelineGroup(TimelineGroupType.YEARLY, group.getDate(), group.getItems()))
                .toList();

        return new TimelineResponse("yearly", groups, groups.size());
    }

    private String formatMonthlyLabel(String yearMonthStr) {
        if (yearMonthStr == null || yearMonthStr.isBlank()) {
            return "Unknown";
        }
        try {
            return YearMonth.parse(yearMonthStr).format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        } catch (Exception e) {
            return yearMonthStr;
        }
    }

    private TimelineGroupResponse buildTimelineGroup(TimelineGroupType type, String label, List<GalleryMediaDocument> docs) {
        if (docs == null) {
            docs = List.of();
        }
        List<MediaItemResponse> items = docs.stream()
                .sorted(Comparator.comparing(GalleryMediaDocument::getUploadedAt).reversed())
                .map(galleryMediaMapper::toMediaItemResponse)
                .toList();

        return timelineMapper.toTimelineGroupResponse(type, label, items);
    }
}
