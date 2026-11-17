package com.example.Gallery.application.service.impl;

import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.TimelineGroupResponse;
import com.example.Gallery.api.dto.response.TimelineResponse;
import com.example.Gallery.application.service.interfaces.ITimelineService;
import com.example.Gallery.infrastructure.mapper.GalleryMediaMapper;
import com.example.Gallery.infrastructure.mapper.TimelineMapper;
import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
import com.example.Gallery.infrastructure.persistence.repository.GalleryMediaRepository;
import com.example.Gallery.infrastructure.persistence.repository.TimelineCountResult;
import com.example.Gallery.infrastructure.persistence.repository.TimelineGroupAggregationResult;
import com.example.Gallery.shared.enums.TimelineGroupType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineServiceImpl implements ITimelineService {

    private final GalleryMediaRepository mediaRepository;
    private final GalleryMediaMapper galleryMediaMapper;
    private final TimelineMapper timelineMapper;

    @Override
    public TimelineResponse getTimeline(String userId, TimelineGroupType groupBy) {
        return getTimeline(userId, groupBy, 0, 10);
    }

    @Override
    public TimelineResponse getTimeline(String userId, TimelineGroupType groupBy, int page, int size) {
        TimelineGroupType groupType = groupBy != null ? groupBy : TimelineGroupType.DAILY;
        return switch (groupType) {
            case DAILY -> getDailyTimeline(userId, page, size);
            case MONTHLY -> getMonthlyTimeline(userId, page, size);
            case YEARLY -> getYearlyTimeline(userId, page, size);
        };
    }

    @Override
    public TimelineResponse getDailyTimeline(String userId) {
        return getDailyTimeline(userId, 0, 10);
    }

    @Override
    public TimelineResponse getDailyTimeline(String userId, int page, int size) {
        log.info("📅 Generating paginated daily timeline for user: {} (page: {}, size: {})", userId, page, size);

        int validPage = Math.max(0, page);
        int validSize = Math.min(50, Math.max(1, size));
        long skip = (long) validPage * validSize;

        List<TimelineGroupAggregationResult> groupedData = mediaRepository.getGroupedTimelineDailyPaginated(userId, skip, validSize);
        TimelineCountResult countResult = mediaRepository.countGroupedTimelineDaily(userId);
        int totalGroups = countResult != null ? (int) countResult.getTotal() : 0;
        boolean hasMore = skip + validSize < totalGroups;

        log.info("Found {} daily timeline groups for user: {} (total: {})", groupedData.size(), userId, totalGroups);

        List<TimelineGroupResponse> groups = groupedData.stream()
                .map(group -> buildTimelineGroup(TimelineGroupType.DAILY, group.getDate(), group.getDate(), group.getCount(), userId))
                .toList();

        return new TimelineResponse("daily", groups, totalGroups, validPage, validSize, hasMore);
    }

    @Override
    public TimelineResponse getMonthlyTimeline(String userId) {
        return getMonthlyTimeline(userId, 0, 10);
    }

    @Override
    public TimelineResponse getMonthlyTimeline(String userId, int page, int size) {
        log.info("📅 Generating paginated monthly timeline for user: {} (page: {}, size: {})", userId, page, size);

        int validPage = Math.max(0, page);
        int validSize = Math.min(50, Math.max(1, size));
        long skip = (long) validPage * validSize;

        List<TimelineGroupAggregationResult> groupedData = mediaRepository.getGroupedTimelineMonthlyPaginated(userId, skip, validSize);
        TimelineCountResult countResult = mediaRepository.countGroupedTimelineMonthly(userId);
        int totalGroups = countResult != null ? (int) countResult.getTotal() : 0;
        boolean hasMore = skip + validSize < totalGroups;

        log.info("Found {} monthly timeline groups for user: {} (total: {})", groupedData.size(), userId, totalGroups);

        List<TimelineGroupResponse> groups = groupedData.stream()
                .map(group -> {
                    String label = formatMonthlyLabel(group.getDate());
                    return buildTimelineGroup(TimelineGroupType.MONTHLY, label, group.getDate(), group.getCount(), userId);
                })
                .toList();

        return new TimelineResponse("monthly", groups, totalGroups, validPage, validSize, hasMore);
    }

    @Override
    public TimelineResponse getYearlyTimeline(String userId) {
        return getYearlyTimeline(userId, 0, 10);
    }

    @Override
    public TimelineResponse getYearlyTimeline(String userId, int page, int size) {
        log.info("📅 Generating paginated yearly timeline for user: {} (page: {}, size: {})", userId, page, size);

        int validPage = Math.max(0, page);
        int validSize = Math.min(50, Math.max(1, size));
        long skip = (long) validPage * validSize;

        List<TimelineGroupAggregationResult> groupedData = mediaRepository.getGroupedTimelineYearlyPaginated(userId, skip, validSize);
        TimelineCountResult countResult = mediaRepository.countGroupedTimelineYearly(userId);
        int totalGroups = countResult != null ? (int) countResult.getTotal() : 0;
        boolean hasMore = skip + validSize < totalGroups;

        log.info("Found {} yearly timeline groups for user: {} (total: {})", groupedData.size(), userId, totalGroups);

        List<TimelineGroupResponse> groups = groupedData.stream()
                .map(group -> buildTimelineGroup(TimelineGroupType.YEARLY, group.getDate(), group.getDate(), group.getCount(), userId))
                .toList();

        return new TimelineResponse("yearly", groups, totalGroups, validPage, validSize, hasMore);
    }

    @Override
    public Page<MediaItemResponse> getGroupItems(String userId, TimelineGroupType groupBy, String groupDate, int page, int size) {
        int validPage = Math.max(0, page);
        int validSize = Math.min(100, Math.max(1, size));
        log.info("Fetching items for user: {}, group: {}, date: {}, page: {}, size: {}", userId, groupBy, groupDate, validPage, validSize);
        Instant[] range = parseDateRange(groupBy, groupDate);
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        Page<GalleryMediaDocument> docPage = mediaRepository
                .findByUserIdAndUploadedAtBetweenAndDeletedAtIsNullOrderByUploadedAtDesc(userId, range[0], range[1], pageable);
        return docPage.map(galleryMediaMapper::toMediaItemResponse);
    }

    private Instant[] parseDateRange(TimelineGroupType type, String dateLabel) {
        Instant start;
        Instant end;
        try {
            switch (type) {
                case DAILY -> {
                    LocalDate date = LocalDate.parse(dateLabel);
                    start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
                    end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
                }
                case MONTHLY -> {
                    YearMonth ym;
                    try {
                        ym = YearMonth.parse(dateLabel);
                    } catch (DateTimeParseException ex) {
                        ym = YearMonth.parse(dateLabel, DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
                    }
                    start = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
                    end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
                }
                case YEARLY -> {
                    Year y = Year.parse(dateLabel);
                    start = y.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
                    end = y.plusYears(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
                }
                default -> {
                    start = Instant.EPOCH;
                    end = Instant.now();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse date range for type {} and label {}: {}", type, dateLabel, e.getMessage());
            start = Instant.EPOCH;
            end = Instant.now();
        }
        return new Instant[]{start, end};
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

    private TimelineGroupResponse buildTimelineGroup(TimelineGroupType type, String label, String dateStr, long totalCount, String userId) {
        Instant[] range = parseDateRange(type, dateStr);
        Pageable initialPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        Page<GalleryMediaDocument> firstPage = mediaRepository
                .findByUserIdAndUploadedAtBetweenAndDeletedAtIsNullOrderByUploadedAtDesc(userId, range[0], range[1], initialPageable);

        List<MediaItemResponse> items = firstPage.getContent().stream()
                .map(galleryMediaMapper::toMediaItemResponse)
                .toList();

        boolean hasMore = totalCount > items.size();
        return timelineMapper.toTimelineGroupResponse(
                type, label, dateStr, items, 0, items.size(), totalCount, hasMore);
    }
}
