package com.example.MediaService.infrastructure.mapper;

import com.example.MediaService.api.dto.response.MediaResponse;
import com.example.MediaService.infrastructure.persistence.document.MediaDocument;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface MediaMapper {

    default MediaResponse toResponse(MediaDocument document) {
        if (document == null) {
            return null;
        }

        return MediaResponse.builder()
                .id(document.getId())
                .userId(document.getUserId())
                .domain(document.getDomain())
                .entityRefId(document.getEntityRefId())
                .fileName(document.getFileName())
                .originalFileName(document.getOriginalFileName())
                .mimeType(document.getMimeType())
                .sizeBytes(document.getSizeBytes())
                .storageKey(document.getStorageKey())
                .checksumSha256(document.getChecksumSha256())
                .metadata(document.getMetadata())
                .uploadedAt(document.getUploadedAt())
                .createdAt(document.getCreatedAt())
                .status(document.isActive() ? "ACTIVE" : "DELETED")
                .build();
    }

    default MediaDocument toDocument(MediaResponse response) {
        if (response == null) {
            return null;
        }

        return MediaDocument.builder()
                .id(response.getId())
                .userId(response.getUserId())
                .domain(response.getDomain())
                .entityRefId(response.getEntityRefId())
                .fileName(response.getFileName())
                .originalFileName(response.getOriginalFileName())
                .mimeType(response.getMimeType())
                .sizeBytes(response.getSizeBytes())
                .storageKey(response.getStorageKey())
                .checksumSha256(response.getChecksumSha256())
                .metadata(response.getMetadata())
                .uploadedAt(response.getUploadedAt())
                .createdAt(response.getCreatedAt())
                .build();
    }
}
