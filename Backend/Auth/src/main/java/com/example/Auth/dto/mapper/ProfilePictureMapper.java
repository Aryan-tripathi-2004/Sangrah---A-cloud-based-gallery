package com.example.Auth.dto.mapper;

import com.example.Auth.dto.response.ProfilePictureResponse;
import com.example.Auth.model.ProfilePictureModel;
import org.springframework.stereotype.Component;

/**
 * Mapper for ProfilePicture-related DTOs.
 */
@Component
public class ProfilePictureMapper {

    /**
     * Convert ProfilePictureModel to ProfilePictureResponse.
     */
    public ProfilePictureResponse toResponse(ProfilePictureModel model) {
        if (model == null) {
            return null;
        }

        ProfilePictureResponse response = new ProfilePictureResponse();
        response.setId(model.getId());
        response.setUserId(model.getUserId());
        response.setStorageKey(model.getStorageKey());
        response.setFilename(model.getOriginalFilename());
        response.setContentType(model.getContentType());
        response.setSizeBytes(model.getSizeBytes());
        response.setWidth(model.getWidth());
        response.setHeight(model.getHeight());
        response.setCurrent(model.isCurrent());
        response.setCreatedAt(model.getCreatedAt());

        // Set computed fields from model
        response.setFormattedSize(formatSize(model.getSizeBytes()));
        response.setAspectRatio(model.getAspectRatio());

        return response;
    }

    /**
     * Helper method to format file size.
     */
    private String formatSize(Long sizeBytes) {
        if (sizeBytes == null || sizeBytes == 0) {
            return "0 B";
        }
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        if (sizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeBytes / 1024.0);
        }
        return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
    }
}
