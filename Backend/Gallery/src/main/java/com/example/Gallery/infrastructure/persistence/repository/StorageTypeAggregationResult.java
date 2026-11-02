package com.example.Gallery.infrastructure.persistence.repository;

public record StorageTypeAggregationResult(
        String _id,
        long totalBytes,
        long count
) {}
