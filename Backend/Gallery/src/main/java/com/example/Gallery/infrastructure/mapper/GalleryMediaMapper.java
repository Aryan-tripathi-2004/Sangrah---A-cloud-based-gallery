package com.example.Gallery.infrastructure.mapper;

import com.example.Gallery.api.dto.request.GalleryMediaRequest;
import com.example.Gallery.api.dto.response.GalleryMediaResponse;
import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for GalleryMedia Entity ↔ DTO conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GalleryMediaMapper {

    /**
     * Convert GalleryMediaDocument to GalleryMediaResponse DTO
     */
    GalleryMediaResponse toResponse(GalleryMediaDocument document);

    /**
     * Convert GalleryMediaRequest DTO to GalleryMediaDocument
     */
    GalleryMediaDocument toDocument(GalleryMediaRequest request);
}
