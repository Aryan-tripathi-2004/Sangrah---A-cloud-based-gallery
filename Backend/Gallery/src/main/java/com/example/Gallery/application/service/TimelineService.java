package com.example.Gallery.application.service;

import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
import com.example.Gallery.infrastructure.persistence.repository.GalleryMediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineService {

    private final GalleryMediaRepository mediaRepository;
    private final MediaService mediaService;

    /**
     * Get daily timeline view (grouped by day)
     */
    public List<Map<String, Object>> getDailyTimeline(String userId) {
        log.info("📅 Generating daily timeline for user: {}", userId);

        List<GalleryMediaDocument> media = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);
        log.info("Found {} active media files", media.size());

        // Group by date
        Map<LocalDate, List<GalleryMediaDocument>> groupedByDay = media.stream()
                .collect(Collectors.groupingBy(m -> m.getUploadedAt()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()));

        // Sort by date descending (newest first)
        return groupedByDay.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .map(entry -> buildTimelineGroup("DAILY", entry.getKey().toString(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Get monthly timeline view (grouped by month)
     */
    public List<Map<String, Object>> getMonthlyTimeline(String userId) {
        log.info("📅 Generating monthly timeline for user: {}", userId);

        List<GalleryMediaDocument> media = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);

        // Group by year-month
        Map<YearMonth, List<GalleryMediaDocument>> groupedByMonth = media.stream()
                .collect(Collectors.groupingBy(m ->
                        YearMonth.from(m.getUploadedAt()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate())));

        // Sort by month descending (newest first)
        return groupedByMonth.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .map(entry -> {
                    String label = entry.getKey().format(
                            java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"));
                    return buildTimelineGroup("MONTHLY", label, entry.getValue());
                })
                .collect(Collectors.toList());
    }

    /**
     * Get yearly timeline view (grouped by year)
     */
    public List<Map<String, Object>> getYearlyTimeline(String userId) {
        log.info("📅 Generating yearly timeline for user: {}", userId);

        List<GalleryMediaDocument> media = mediaRepository.findByUserIdAndDeletedAtIsNull(userId);

        // Group by year
        Map<Integer, List<GalleryMediaDocument>> groupedByYear = media.stream()
                .collect(Collectors.groupingBy(m ->
                        m.getUploadedAt()
                                .atZone(ZoneId.systemDefault())
                                .getYear()));

        // Sort by year descending (newest first)
        return groupedByYear.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .map(entry -> buildTimelineGroup("YEARLY", String.valueOf(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    // ============ HELPER METHODS ============

    /**
     * Build a timeline group with header and media items
     */
    private Map<String, Object> buildTimelineGroup(String type, String label, List<GalleryMediaDocument> docs) {
        List<MediaItemResponse> items = docs.stream()
                .sorted(Comparator.comparing(GalleryMediaDocument::getUploadedAt).reversed())
                .map(doc -> MediaItemResponse.builder()
                        .id(doc.getId())
                        .originalFileName(doc.getOriginalFileName())
                        .mimeType(doc.getMimeType())
                        .sizeBytes(doc.getSizeBytes())
                        .type(doc.getType())
                        .checksumSha256(doc.getChecksumSha256())
                        .uploadedAt(doc.getUploadedAt())
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> group = new LinkedHashMap<>();
        group.put("type", "GROUP");
        group.put("groupType", type);
        group.put("label", label);
        group.put("count", items.size());
        group.put("items", items);

        return group;
    }
}
