package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.request.EventRequest;
import com.example.Event.api.dto.response.EventResponse;
import com.example.Event.infrastructure.persistence.document.EventDocument;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Event Entity ↔ DTO conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMapper {

    /**
     * Convert EventDocument to EventResponse DTO
     */
    EventResponse toResponse(EventDocument document);

    /**
     * Convert EventRequest DTO to EventDocument
     */
    EventDocument toDocument(EventRequest request);
}
