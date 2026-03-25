package com.example.Event.infrastructure.mapper;

import com.example.Event.api.dto.request.EventMediaRequest;
import com.example.Event.api.dto.response.EventMediaResponse;
import com.example.Event.infrastructure.persistence.document.EventMediaDocument;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for EventMedia Entity ↔ DTO conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventMediaMapper {

    /**
     * Convert EventMediaDocument to EventMediaResponse DTO
     */
    EventMediaResponse toResponse(EventMediaDocument document);

    /**
     * Convert EventMediaRequest DTO to EventMediaDocument
     */
    EventMediaDocument toDocument(EventMediaRequest request);
}
