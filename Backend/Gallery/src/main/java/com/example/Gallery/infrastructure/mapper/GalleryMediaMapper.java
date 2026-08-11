package com.example.Gallery.infrastructure.mapper;

import com.example.Gallery.api.dto.request.GalleryMediaRequest;
import com.example.Gallery.api.dto.response.GalleryMediaResponse;
import com.example.Gallery.api.dto.response.MediaItemResponse;
import com.example.Gallery.infrastructure.persistence.document.GalleryMediaDocument;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GalleryMediaMapper {

    @Mapping(target = "mediaId", source = "id")
    @Mapping(target = "ownerId", source = "ownerUserId")
    @Mapping(target = "fileName", source = "originalFileName")
    @Mapping(target = "mediaType", source = "type")
    @Mapping(target = "fileSize", source = "sizeBytes")
    @Mapping(target = "storagePath", source = "storageKey")
    GalleryMediaResponse toResponse(GalleryMediaDocument document);

    @Mapping(target = "originalFileName", source = "fileName")
    @Mapping(target = "type", source = "mediaType")
    @Mapping(target = "sizeBytes", source = "fileSize")
    GalleryMediaDocument toDocument(GalleryMediaRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "visibilityStatus", source = "visibility")
    MediaItemResponse toMediaItemResponse(GalleryMediaDocument document);

    default LocalDateTime map(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    default Instant map(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
