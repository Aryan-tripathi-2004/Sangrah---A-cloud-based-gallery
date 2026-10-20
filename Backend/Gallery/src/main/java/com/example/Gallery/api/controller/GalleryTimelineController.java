package com.example.Gallery.api.controller;

import com.example.Gallery.api.annotation.CurrentUserId;
import com.example.Gallery.api.dto.response.TimelineResponse;
import com.example.Gallery.application.service.interfaces.ITimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.Gallery.shared.enums.TimelineGroupType;

@Slf4j
@RestController
@RequestMapping("/api/v1/gallery/timeline")
@RequiredArgsConstructor
@Tag(name = "Gallery Timeline", description = "Timeline views (daily, monthly, yearly) for media organization")
public class GalleryTimelineController {

    private final ITimelineService timelineService;

    @GetMapping
    @Operation(summary = "Get timeline", description = "View media grouped by specified interval (newest first)")
    public ResponseEntity<TimelineResponse> getTimeline(
            @CurrentUserId String userId,
            @RequestParam(defaultValue = "DAILY") TimelineGroupType groupBy) {
            
        log.info("📅 Timeline request from user: {} grouping by: {}", userId, groupBy);
        
        return switch (groupBy) {
            case DAILY -> ResponseEntity.ok(timelineService.getDailyTimeline(userId));
            case MONTHLY -> ResponseEntity.ok(timelineService.getMonthlyTimeline(userId));
            case YEARLY -> ResponseEntity.ok(timelineService.getYearlyTimeline(userId));
        };
    }
}
