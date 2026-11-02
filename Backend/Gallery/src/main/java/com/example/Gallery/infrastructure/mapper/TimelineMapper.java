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
    @Mapping(target = "count", expression = "java(items != null ? items.size() : 0)")
    @Mapping(target = "items", source = "items")
    TimelineGroupResponse toTimelineGroupResponse(TimelineGroupType type, String label, List<MediaItemResponse> items);
}
