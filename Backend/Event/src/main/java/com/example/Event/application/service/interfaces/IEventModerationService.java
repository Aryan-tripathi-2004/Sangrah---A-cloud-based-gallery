package com.example.Event.application.service.interfaces;

import com.example.Event.api.dto.request.MediaRejectionRequest;
import com.example.Event.api.dto.response.EventMediaCollectionResponse;
import com.example.Event.api.dto.response.EventMediaDetailResponse;
import com.example.Event.api.dto.response.EventMediaFileResponse;
import com.example.Event.api.dto.response.EventMediaModerationResponse;
import com.example.Event.api.dto.response.EventMediaUploadResponse;
import com.example.Event.api.dto.response.MessageResponse;
import com.example.Event.shared.enums.ApprovalStatus;
import org.springframework.web.multipart.MultipartFile;

public interface IEventModerationService {
    EventMediaFileResponse getMediaFile(String eventId, String mediaId, String userId);

    EventMediaUploadResponse uploadMedia(String eventId, MultipartFile file, String userId, String userEmail);

    EventMediaCollectionResponse getTimeline(String eventId, String userId);

    EventMediaCollectionResponse listEventMedia(String eventId, String userId);

    EventMediaDetailResponse getMedia(String eventId, String mediaId);

    EventMediaModerationResponse approveMedia(String eventId, String mediaId, String userId);

    EventMediaModerationResponse rejectMedia(String eventId, String mediaId, MediaRejectionRequest request, String userId);

    MessageResponse deleteMedia(String eventId, String mediaId, String userId);

    ApprovalStatus createMedia(String eventId, String mediaId, String uploaderUserId, boolean moderationEnabled);

    ApprovalStatus getMediaStatus(String eventId, String mediaId);

    boolean isMediaApproved(String eventId, String mediaId);
}
