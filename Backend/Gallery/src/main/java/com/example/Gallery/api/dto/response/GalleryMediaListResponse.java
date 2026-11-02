package com.example.Gallery.api.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record GalleryMediaListResponse(
    long count,
    List<MediaItemResponse> items
) {}
