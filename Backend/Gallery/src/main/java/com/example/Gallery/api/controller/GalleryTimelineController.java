package com.example.Gallery.api.controller;

import com.example.Gallery.api.annotation.CurrentUserId;
import com.example.Gallery.api.dto.response.TimelineGroupResponse;
import com.example.Gallery.application.service.interfaces.ITimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/gallery/timeline")
@RequiredArgsConstructor
@Tag(name = "Gallery Timeline", description = "Timeline views (daily, monthly, yearly) for media organization")
public class GalleryTimelineController {

    private final ITimelineService timelineService;

    @GetMapping("/daily")
    @Operation(summary = "Daily timeline", description = "View media grouped by day (newest first)")
    public ResponseEntity<List<TimelineGroupResponse>> getDailyTimeline(@CurrentUserId String userId) {
        log.info("📅 Daily timeline request from user: {}", userId);
        List<TimelineGroupResponse> timeline = timelineService.getDailyTimeline(userId);
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/monthly")
    @Operation(summary = "Monthly timeline", description = "View media grouped by month (newest first)")
    public ResponseEntity<List<TimelineGroupResponse>> getMonthlyTimeline(@CurrentUserId String userId) {
        log.info("📅 Monthly timeline request from user: {}", userId);
        List<TimelineGroupResponse> timeline = timelineService.getMonthlyTimeline(userId);
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/yearly")
    @Operation(summary = "Yearly timeline", description = "View media grouped by year (newest first)")
    public ResponseEntity<List<TimelineGroupResponse>> getYearlyTimeline(@CurrentUserId String userId) {
        log.info("📅 Yearly timeline request from user: {}", userId);
        List<TimelineGroupResponse> timeline = timelineService.getYearlyTimeline(userId);
        return ResponseEntity.ok(timeline);
    }
}
