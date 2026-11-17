package com.example.Gallery.api.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

@Builder
public record GalleryMediaListResponse(
    long count,
    List<MediaItemResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last,
    boolean hasNext
) {
    public GalleryMediaListResponse(long count, List<MediaItemResponse> items) {
        this(count, items, 0, items != null ? items.size() : 0, count, 1, true, true, false);
    }

    public static GalleryMediaListResponse from(Page<MediaItemResponse> page) {
        return new GalleryMediaListResponse(
            page.getTotalElements(),
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast(),
            page.hasNext()
        );
    }
}
