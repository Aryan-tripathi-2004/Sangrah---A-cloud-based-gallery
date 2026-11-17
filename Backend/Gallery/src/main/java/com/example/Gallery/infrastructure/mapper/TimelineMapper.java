package com.example.Gallery.infrastructure.mapper;

import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.TimelineGroupResponse;
import com.example.Gallery.shared.enums.TimelineGroupType;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface TimelineMapper {

    @Mapping(target = "type", expression = "java(type != null ? type.name() : null)")
    @Mapping(target = "groupType", source = "type")
    @Mapping(target = "label", source = "label")
    @Mapping(target = "date", source = "label")
    @Mapping(target = "count", expression = "java(items != null ? items.size() : 0)")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "page", constant = "0")
    @Mapping(target = "size", expression = "java(items != null ? items.size() : 0)")
    @Mapping(target = "totalElements", expression = "java(items != null ? (long) items.size() : 0L)")
    @Mapping(target = "hasMore", constant = "false")
    TimelineGroupResponse toTimelineGroupResponse(TimelineGroupType type, String label, List<MediaItemResponse> items);

    @Mapping(target = "type", expression = "java(type != null ? type.name() : null)")
    @Mapping(target = "groupType", source = "type")
    @Mapping(target = "label", source = "label")
    @Mapping(target = "date", source = "date")
    @Mapping(target = "count", expression = "java(items != null ? items.size() : 0)")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "page", source = "page")
    @Mapping(target = "size", source = "size")
    @Mapping(target = "totalElements", source = "totalElements")
    @Mapping(target = "hasMore", source = "hasMore")
    TimelineGroupResponse toTimelineGroupResponse(
        TimelineGroupType type, String label, String date, List<MediaItemResponse> items,
        int page, int size, long totalElements, boolean hasMore);
}
