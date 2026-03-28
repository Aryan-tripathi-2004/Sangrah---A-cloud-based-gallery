package com.example.Gallery.api.controller;

import com.example.Gallery.application.service.TimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/gallery/timeline")
@RequiredArgsConstructor
@Tag(name = "Gallery Timeline", description = "Timeline views (daily, monthly, yearly) for media organization")
public class GalleryTimelineController {

    private final TimelineService timelineService;

    /**
     * Get daily timeline view
     * GET /api/v1/gallery/timeline/daily
     * Groups media by day, newest first
     */
    @GetMapping("/daily")
    @Operation(summary = "Daily timeline", description = "View media grouped by day (newest first)")
    public ResponseEntity<?> getDailyTimeline(HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("📅 Daily timeline request from user: {}", userId);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found in context"));
            }

            List<Map<String, Object>> timeline = timelineService.getDailyTimeline(userId);
            return ResponseEntity.ok(Map.of(
                    "view", "DAILY",
                    "groups", timeline,
                    "totalGroups", timeline.size()
            ));

        } catch (Exception e) {
            log.error("❌ Daily timeline error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get daily timeline"));
        }
    }

    /**
     * Get monthly timeline view
     * GET /api/v1/gallery/timeline/monthly
     * Groups media by month, newest first
     */
    @GetMapping("/monthly")
    @Operation(summary = "Monthly timeline", description = "View media grouped by month (newest first)")
    public ResponseEntity<?> getMonthlyTimeline(HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("📅 Monthly timeline request from user: {}", userId);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found in context"));
            }

            List<Map<String, Object>> timeline = timelineService.getMonthlyTimeline(userId);
            return ResponseEntity.ok(Map.of(
                    "view", "MONTHLY",
                    "groups", timeline,
                    "totalGroups", timeline.size()
            ));

        } catch (Exception e) {
            log.error("❌ Monthly timeline error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get monthly timeline"));
        }
    }

    /**
     * Get yearly timeline view
     * GET /api/v1/gallery/timeline/yearly
     * Groups media by year, newest first
     */
    @GetMapping("/yearly")
    @Operation(summary = "Yearly timeline", description = "View media grouped by year (newest first)")
    public ResponseEntity<?> getYearlyTimeline(HttpServletRequest request) {
        try {
            String userId = request.getHeader("X-User-Id");
            log.info("📅 Yearly timeline request from user: {}", userId);

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User ID not found in context"));
            }

            List<Map<String, Object>> timeline = timelineService.getYearlyTimeline(userId);
            return ResponseEntity.ok(Map.of(
                    "view", "YEARLY",
                    "groups", timeline,
                    "totalGroups", timeline.size()
            ));

        } catch (Exception e) {
            log.error("❌ Yearly timeline error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get yearly timeline"));
        }
    }
}
