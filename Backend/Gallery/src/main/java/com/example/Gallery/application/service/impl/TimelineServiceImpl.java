package com.example.Gallery.application.service.impl;

import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.TimelineGroupResponse;
import com.example.Gallery.application.service.interfaces.ITimelineService;
import com.example.Gallery.infrastructure.mapper.GalleryMediaMapper;
import com.example.Gallery.infrastructure.mapper.TimelineMapper;
import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
import com.example.Gallery.infrastructure.persistence.repository.GalleryMediaRepository;
import com.example.Gallery.shared.enums.TimelineGroupType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineServiceImpl implements ITimelineService {

    private final GalleryMediaRepository mediaRepository;
    private final GalleryMediaMapper galleryMediaMapper;
    private final TimelineMapper timelineMapper;

    @Override
    public List<TimelineGroupResponse> getDailyTimeline(String userId) {
        log.info("📅 Generating daily timeline for user: {}", userId);

        List<GalleryMediaDocument> media = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        log.info("Found {} active media files", media.size());

        Map<LocalDate, List<GalleryMediaDocument>> groupedByDay = media.stream()
                .collect(Collectors.groupingBy(m -> m.getUploadedAt()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()));

        return groupedByDay.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .map(entry -> buildTimelineGroup(TimelineGroupType.DAILY, entry.getKey().toString(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TimelineGroupResponse> getMonthlyTimeline(String userId) {
        log.info("📅 Generating monthly timeline for user: {}", userId);

        List<GalleryMediaDocument> media = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);

        Map<YearMonth, List<GalleryMediaDocument>> groupedByMonth = media.stream()
                .collect(Collectors.groupingBy(m ->
                        YearMonth.from(m.getUploadedAt()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate())));

        return groupedByMonth.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .map(entry -> {
                    String label = entry.getKey().format(
                            java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"));
                    return buildTimelineGroup(TimelineGroupType.MONTHLY, label, entry.getValue());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TimelineGroupResponse> getYearlyTimeline(String userId) {
        log.info("📅 Generating yearly timeline for user: {}", userId);

        List<GalleryMediaDocument> media = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);

        Map<Integer, List<GalleryMediaDocument>> groupedByYear = media.stream()
                .collect(Collectors.groupingBy(m ->
                        m.getUploadedAt()
                                .atZone(ZoneId.systemDefault())
                                .getYear()));

        return groupedByYear.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .map(entry -> buildTimelineGroup(TimelineGroupType.YEARLY, String.valueOf(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    private TimelineGroupResponse buildTimelineGroup(TimelineGroupType type, String label, List<GalleryMediaDocument> docs) {
        List<MediaItemResponse> items = docs.stream()
                .sorted(Comparator.comparing(GalleryMediaDocument::getUploadedAt).reversed())
                .map(galleryMediaMapper::toMediaItemResponse)
                .collect(Collectors.toList());

        return timelineMapper.toTimelineGroupResponse(type, label, items);
    }
}
