package com.example.Gallery.infrastructure.persistence.repository;

import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.List;

/**
 * Projection DTO for MongoDB aggregation pipeline results
 * grouping gallery media by time intervals (daily, monthly, yearly).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineGroupAggregationResult {

    @Id
    private String id;

    private List<GalleryMediaDocument> items;

    public String getDate() {
        return id;
    }
}
