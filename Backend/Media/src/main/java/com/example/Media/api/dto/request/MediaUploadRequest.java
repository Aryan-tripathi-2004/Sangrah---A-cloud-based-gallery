package com.example.Media.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaUploadRequest {
    private String fileName;
    private String domain;  // "GALLERY", "EVENTS", "PROFILE_AVATAR", "MESSAGING"
    private String entityRefId;  // Optional - for events, profiles, etc.
    private Long sizeBytes;
    private String checksumSha256;
    private Map<String, Object> metadata;
}
