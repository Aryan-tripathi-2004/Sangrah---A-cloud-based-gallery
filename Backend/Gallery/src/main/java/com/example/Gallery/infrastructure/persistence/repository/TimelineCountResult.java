package com.example.Gallery.infrastructure.persistence.repository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Projection DTO for MongoDB aggregation counting distinct timeline groups.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimelineCountResult {
    private long total;
}
