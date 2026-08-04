package com.example.Event.application.service.interfaces;

import com.example.Event.api.dto.request.MediaRejectionRequest;
import com.example.Event.api.dto.response.EventMediaCollectionResponse;
import com.example.Event.api.dto.response.EventMediaDetailResponse;
import com.example.Event.api.dto.response.EventMediaFileResponse;
import com.example.Event.api.dto.response.EventMediaModerationResponse;
import com.example.Event.api.dto.response.EventMediaUploadResponse;
import com.example.Event.api.dto.response.MessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

public interface IEventModerationService {
    EventMediaFileResponse getMediaFile(String eventId, String mediaId, HttpServletRequest httpRequest);

    EventMediaUploadResponse uploadMedia(String eventId, MultipartFile file, HttpServletRequest httpRequest);

    EventMediaCollectionResponse getTimeline(String eventId, HttpServletRequest httpRequest);

    EventMediaCollectionResponse listEventMedia(String eventId, HttpServletRequest httpRequest);

    EventMediaDetailResponse getMedia(String eventId, String mediaId);

    EventMediaModerationResponse approveMedia(String eventId, String mediaId, HttpServletRequest httpRequest);

    EventMediaModerationResponse rejectMedia(String eventId, String mediaId, MediaRejectionRequest request, HttpServletRequest httpRequest);

    MessageResponse deleteMedia(String eventId, String mediaId, HttpServletRequest httpRequest);

    String createMedia(String eventId, String mediaId, String uploaderUserId, boolean moderationEnabled);

    String getMediaStatus(String eventId, String mediaId);

    boolean isMediaApproved(String eventId, String mediaId);
}
