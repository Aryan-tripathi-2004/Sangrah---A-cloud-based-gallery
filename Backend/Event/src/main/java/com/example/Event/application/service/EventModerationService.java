package com.example.Event.application.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EventModerationService {
    private final Map<String, Boolean> moderationByEvent = new ConcurrentHashMap<>();
    private final Map<String, String> accessStatusByRequest = new ConcurrentHashMap<>();
    private final Map<String, String> mediaStatusByMediaId = new ConcurrentHashMap<>();

    public String createAccessRequest(String requestId) {
        accessStatusByRequest.put(requestId, "PENDING");
        return "PENDING";
    }

    public String approveAccess(String requestId) {
        accessStatusByRequest.put(requestId, "APPROVED");
        return "APPROVED";
    }

    public String rejectAccess(String requestId) {
        accessStatusByRequest.put(requestId, "REJECTED");
        return "REJECTED";
    }

    public String createMedia(String eventId, String mediaId) {
        String status = moderationByEvent.getOrDefault(eventId, true) ? "PENDING" : "APPROVED";
        mediaStatusByMediaId.put(mediaId, status);
        return status;
    }

    public String approveMedia(String mediaId) {
        mediaStatusByMediaId.put(mediaId, "APPROVED");
        return "APPROVED";
    }

    public String rejectMedia(String mediaId) {
        mediaStatusByMediaId.put(mediaId, "REJECTED");
        return "REJECTED";
    }

    public boolean setModeration(String eventId, boolean enabled) {
        moderationByEvent.put(eventId, enabled);
        return enabled;
    }

    public boolean getModeration(String eventId) {
        return moderationByEvent.getOrDefault(eventId, true);
    }
}
