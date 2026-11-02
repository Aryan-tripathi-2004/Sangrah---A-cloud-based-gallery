package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.response.EventInternalDTO;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface EventInternalMapper {
    EventInternalDTO toDTO(EventDocument document);
}
