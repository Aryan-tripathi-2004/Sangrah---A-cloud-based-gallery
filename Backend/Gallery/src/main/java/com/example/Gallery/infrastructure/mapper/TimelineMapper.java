package com.example.Gallery.infrastructure.mapper;

import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.api.dto.response.TimelineGroupResponse;
import com.example.Gallery.shared.enums.TimelineGroupType;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface TimelineMapper {
    
    default TimelineGroupResponse toTimelineGroupResponse(TimelineGroupType type, String label, List<MediaItemResponse> items) {
        return new TimelineGroupResponse(type, label, items.size(), items);
    }
}
